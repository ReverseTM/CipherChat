package ru.mai.khasanov.cipherchat.vaadin.view.chat;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.mai.khasanov.cipherchat.cryptography.CipherFactory;
import ru.mai.khasanov.cipherchat.cryptography.CipherService;
import ru.mai.khasanov.cipherchat.cryptography.DiffieHellmanAlgorithm;
import ru.mai.khasanov.cipherchat.kafka.KafkaReader;
import ru.mai.khasanov.cipherchat.kafka.KafkaWriter;
import ru.mai.khasanov.cipherchat.model.Room;
import ru.mai.khasanov.cipherchat.model.RoomCipherInfo;
import ru.mai.khasanov.cipherchat.model.User;
import ru.mai.khasanov.cipherchat.model.message.ContentMessage;
import ru.mai.khasanov.cipherchat.model.message.KafkaMessage;
import ru.mai.khasanov.cipherchat.service.RoomService;
import ru.mai.khasanov.cipherchat.service.ServerService;
import ru.mai.khasanov.cipherchat.service.UserService;
import ru.mai.khasanov.cipherchat.vaadin.Broadcaster;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Route("user/:userId/room/:roomId")
public class ChatView extends HorizontalLayout implements BeforeEnterObserver {
    private User user;
    private Room room;

    private RoomCipherInfo cipherInfo;

    private final ServerService serverService;
    private final UserService userService;
    private final RoomService roomService;

    private final KafkaWriter kafkaWriter;
    private final KafkaReader kafkaReader;
    private String outputTopic;

    private Registration broadcasterRegistration;

    private Frontend frontend;
    private Backend backend;

    private boolean disconnected;

    public ChatView(
            ServerService serverService,
            UserService userService,
            RoomService roomService,
            KafkaWriter kafkaWriter, KafkaReader kafkaReader) {
        this.serverService = serverService;
        this.userService = userService;
        this.roomService = roomService;
        this.kafkaWriter = kafkaWriter;
        this.kafkaReader = kafkaReader;
        this.disconnected = false;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        Optional<Long> userIdOptional = beforeEnterEvent.getRouteParameters().getLong("userId");
        Optional<Long> roomIdOptional = beforeEnterEvent.getRouteParameters().getLong("roomId");

        if (userIdOptional.isPresent() && roomIdOptional.isPresent()) {
            Optional<User> userOptional = userService.getUserById(userIdOptional.get());
            Optional<Room> roomOptional = roomService.getRoomById(roomIdOptional.get());

            if (userOptional.isPresent() && roomOptional.isPresent()) {
                this.user = userOptional.get();
                this.room = roomOptional.get();

                if (this.room.getUsers().contains(this.user)) {
                    this.broadcasterRegistration = Broadcaster.register(this::receiveBroadcasterMessage);
                    this.cipherInfo = this.room.getRoomCipherInfo();

                    this.backend = new Backend();
                    this.frontend = new Frontend();

                    return;
                }
            }
        }

        disconnected = true;
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);

        kafkaReader.stop();

        if (outputTopic != null) {
            KafkaMessage kafkaMessage = new KafkaMessage(KafkaMessage.Action.CLEAR_MESSAGES, null);
            kafkaWriter.write(kafkaMessage.toBytes(), outputTopic);
        }

        if (!disconnected) {
            if (backend.disconnect()) {
                Broadcaster.broadcast("update");
            }
        }

        if (backend != null && backend.cipherService != null) {
            backend.cipherService.close();
        }

        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
        }
    }

    private void receiveBroadcasterMessage(String message) {
        if (message.contains("delete_room")) {
            long roomId = Long.parseLong(message.split("_")[2]);

            if (roomId == this.room.getId()) {
                disconnected = true;
                updateUI(this::navigateToMainView);
            }
        }
    }

    private void navigateToMainView() {
        UI.getCurrent().navigate(MainView.class, new RouteParameters("userId", String.valueOf(user.getId())));
    }

    private void notifyUser(String message) {
        updateUI(() -> Notification.show(message));
    }

    private void updateUI(Command command) {
        Optional<UI> maybeUI = getUI();
        maybeUI.ifPresent(ui -> ui.access(command));

    }

    private class Frontend {
        public enum Destination {
            PRODUCER,
            CONSUMER
        }

        private final VerticalLayout messageContainer;

        public Frontend() {
            addClassNames("chat-view", LumoUtility.Width.FULL, LumoUtility.Display.FLEX, LumoUtility.Flex.AUTO);
            setSpacing(false);
            setSizeFull();

            VerticalLayout roomContainer = new VerticalLayout();

            HorizontalLayout header = createRoomHeaderContent();
            this.messageContainer = createRoomMessageContent();
            HorizontalLayout inputContainer = createRoomInputContent();

            roomContainer.add(header, messageContainer, inputContainer);
            roomContainer.expand(messageContainer);

            add(roomContainer);

        }

        private HorizontalLayout createRoomHeaderContent() {
            H2 roomName = new H2(room.getName());

            MenuBar menuBar = new MenuBar();
            menuBar.setOpenOnHover(true);
            MenuItem members = menuBar.addItem(VaadinIcon.USERS.create());

            Set<User> users = room.getUsers();
            if (users != null && !users.isEmpty()) {
                SubMenu subMenu = members.getSubMenu();
                for (User user : users) {
                    subMenu.addItem(user.getUsername());
                }
            }

            Button disconnectButton = new Button("Disconnect", event -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();

                confirmDialog.setHeader("Disconnect from room");
                confirmDialog.setText("Do you want to disconnect from this room?");

                confirmDialog.setCancelable(true);
                confirmDialog.setConfirmText("Disconnect");
                confirmDialog.addConfirmListener(e -> {
                    if (backend.disconnect()) {
                        disconnected = true;
                        notifyUser("You have been successfully disconnected from the room");
                        Broadcaster.broadcast("update");
                        navigateToMainView();
                    } else {
                        notifyUser("Failed to disconnect from room");
                    }
                });

                confirmDialog.open();
            });
            disconnectButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

            HorizontalLayout header = new HorizontalLayout();

            header.setWidthFull();
            header.setAlignItems(Alignment.CENTER);
            header.getStyle().set("border-bottom", "1px solid #394f64");

            header.add(roomName, menuBar, disconnectButton);
            header.expand(menuBar);

            return header;
        }

        private VerticalLayout createRoomMessageContent() {
            VerticalLayout messageContainer = new VerticalLayout();
            messageContainer.getStyle().set("overflow-y", "auto");
            messageContainer.setSizeFull();

            return messageContainer;
        }

        private HorizontalLayout createRoomInputContent() {
            MessageInput messageInput = new MessageInput();
            Upload upload = createUpload();

            messageInput.getElement().getStyle().set("overflow-y", "auto");

            messageInput.addSubmitListener(event -> {
                String text = event.getValue();

                ContentMessage message = new ContentMessage(ContentMessage.Type.TEXT, text.getBytes(), null);

                showTextMessage(text, Destination.PRODUCER);
                backend.sendMessage(message);
            });

            HorizontalLayout horizontalLayout = new HorizontalLayout();
            horizontalLayout.setWidthFull();
            horizontalLayout.addAndExpand(messageInput);

            HorizontalLayout inputContainer = new HorizontalLayout();
            inputContainer.setAlignItems(Alignment.STRETCH);
            inputContainer.setHeight("100px");

            inputContainer.setWidthFull();
            inputContainer.add(horizontalLayout, upload);
            inputContainer.expand(horizontalLayout);

            return inputContainer;
        }

        private Upload createUpload() {
            MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
            Upload upload = new Upload(buffer);

            upload.setAutoUpload(false);
            upload.setDropAllowed(false);

            upload.addSucceededListener(event -> {
                String filename = event.getFileName();
                String type = event.getMIMEType();

                handleFileMessage(filename, type, buffer.getInputStream(filename));
            });

            return upload;
        }

        private void showTextMessage(String message, Destination destination) {
            updateUI(() -> {
                HorizontalLayout messageContent = new HorizontalLayout();
                messageContent.getStyle()
                        .set("display", "flex")
                        .set("align-items", "flex-end")
                        .set("max-width", "100%");
                messageContent.setSpacing(true);

                Span messageSpan = new Span(message);
                messageSpan.getStyle()
                        .set("font-size", "20px")
                        .set("white-space", "normal")
                        .set("overflow-wrap", "break-word")
                        .set("flex-grow", "1")
                        .set("min-width", "0");

                Span timeSpan = new Span(getCurrentTime());
                timeSpan.getStyle()
                        .set("font-size", "12px")
                        .set("color", "#888")
                        .set("margin-left", "7px")
                        .set("flex-shrink", "0");

                messageContent.add(messageSpan, timeSpan);

                Div div = new Div(messageContent);
                div.getStyle()
                        .set("border-radius", "12px")
                        .set("padding", "5px")
                        .set("max-width", "100%")
                        .set("box-sizing", "border-box");

                if (destination.equals(Destination.PRODUCER)) {
                    div.getStyle().set("background-color", "#2b5377");
                } else {
                    div.getStyle().set("background-color", "#182633");
                }

                this.messageContainer.add(div);
            });

        }

        private void showImageMessage(String filename, byte[] imageData) {
            updateUI(() -> {
                StreamResource imageResource = new StreamResource(filename, () -> new ByteArrayInputStream(imageData));
                Image image = new Image(imageResource, "image");

                image.setWidth("420px");
                image.setHeight("360px");
                image.getStyle()
                        .set("border", "1px solid #ccc")
                        .set("border-radius", "25px");

                Div div = new Div(image);

                this.messageContainer.add(div);
            });
        }

        private void showFileMessage(String filename, byte[] fileData, Destination destination) {
            updateUI(() -> {
                HorizontalLayout messageContent = new HorizontalLayout();
                messageContent.getStyle()
                        .set("display", "flex")
                        .set("align-items", "flex-end")
                        .set("max-width", "100%");
                messageContent.setSpacing(true);

                Anchor file = new Anchor(new StreamResource(filename, () -> new ByteArrayInputStream(fileData)), filename);
                file.getElement().setAttribute("download", true);
                file.getStyle()
                        .set("font-size", "20px")
                        .set("white-space", "normal")
                        .set("overflow-wrap", "break-word")
                        .set("flex-grow", "1")
                        .set("min-width", "0");

                Span timeSpan = new Span(getCurrentTime());
                timeSpan.getStyle()
                        .set("font-size", "12px")
                        .set("color", "#888")
                        .set("margin-left", "7px")
                        .set("flex-shrink", "0");

                messageContent.add(file, timeSpan);

                Div div = new Div(messageContent);

                div.getStyle()
                        .set("border-radius", "12px")
                        .set("padding", "5px")
                        .set("max-width", "100%")
                        .set("box-sizing", "border-box");

                if (destination.equals(Destination.PRODUCER)) {
                    div.getStyle().set("background-color", "#2b5377");
                } else {
                    div.getStyle().set("background-color", "#182633");
                }

                this.messageContainer.add(div);
            });
        }

        private void clearMessages() {
            updateUI(this.messageContainer::removeAll);
        }

        private void handleFileMessage(String filename, String type, InputStream inputStream) {
            try {
                byte[] data = readInputSteam(inputStream);

                ContentMessage message;
                if (isImage(type)) {
                    message = new ContentMessage(ContentMessage.Type.IMAGE, data, filename);
                    showImageMessage(filename, data);
                } else {
                    message = new ContentMessage(ContentMessage.Type.FILE, data, filename);
                    showFileMessage(filename, data, Destination.PRODUCER);
                }

                backend.sendMessage(message);
            } catch (IOException e) {
                notifyUser("Something wrong");
            }
        }

        private byte[] readInputSteam(InputStream stream) throws IOException {
            ByteArrayOutputStream data = new ByteArrayOutputStream();

            int nRead;
            byte[] buffer = new byte[1024];

            while ((nRead = stream.read(buffer, 0, buffer.length)) != -1) {
                data.write(buffer, 0, nRead);
            }

            data.flush();
            return data.toByteArray();
        }

        private String getCurrentTime() {
            LocalTime currentTime = LocalTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            return currentTime.format(formatter);
        }

        private boolean isImage(String type) {
            return type.startsWith("image/");
        }
    }

    private class Backend {
        private CipherService cipherService;

        private byte[] ownPrivateKey;

        public Backend() {
            String inputTopic = String.format("input_room_%s_user_%s", room.getId(), user.getId());

            kafkaReader.subscribe(inputTopic);
            kafkaReader.addListener(this::handleKafkaMessage);
            kafkaReader.startKafkaConsumer();
        }

        private boolean disconnect() {
            return serverService.disconnectRoom(user.getId(), room.getId());
        }

        private void handleKafkaMessage(ConsumerRecord<byte[], byte[]> consumerRecord) {
            KafkaMessage message = KafkaMessage.toKafkaMessage(new String(consumerRecord.value()));

            switch (message.action()) {
                case SETUP_CONNECTION -> {
                    if (message.content() instanceof Long anotherUserId) {
                        outputTopic = String.format("input_room_%s_user_%s", room.getId(), anotherUserId);
                        exchangePublicKey();
                    }
                }

                case EXCHANGE_PUBLIC_KEY -> {
                    if (message.content() instanceof byte[] anotherPublicKey) {
                        setSharedPrivateKey(anotherPublicKey);
                    }
                }

                case CLEAR_MESSAGES -> frontend.clearMessages();

                case MESSAGE -> {
                    if (message.content() instanceof byte[] acceptedMessage) {
                        handleMessage(acceptedMessage);
                    }
                }

                default -> throw new IllegalStateException("Unexpected type");
            }
        }

        private void exchangePublicKey() {
            ownPrivateKey = DiffieHellmanAlgorithm.generateOwnPrivateKey();
            byte[] ownPublicKey = DiffieHellmanAlgorithm.generateOwnPublicKey(
                    ownPrivateKey,
                    cipherInfo.getG(),
                    cipherInfo.getP());

            KafkaMessage keyMessage = new KafkaMessage(KafkaMessage.Action.EXCHANGE_PUBLIC_KEY, ownPublicKey);
            kafkaWriter.write(keyMessage.toBytes(), outputTopic);
        }

        private void setSharedPrivateKey(byte[] anotherPublicKey) {
            byte[] sharedPrivateKey = DiffieHellmanAlgorithm.calculateSharedPrivateKey(
                    anotherPublicKey,
                    ownPrivateKey,
                    cipherInfo.getP());

            this.cipherService = CipherFactory.createCipherService(cipherInfo, sharedPrivateKey);

            notifyUser("Connection established");
        }

        private void handleMessage(byte[] message) {
            if (cipherService != null) {
                CompletableFuture<byte[]> decryptedMessageFuture = cipherService.decrypt(message);

                decryptedMessageFuture.thenAccept(decryptedMessage -> {
                    ContentMessage contentMessage = ContentMessage.toContentMessage(new String(decryptedMessage));

                    byte[] messageBytes = contentMessage.message();

                    switch (contentMessage.messageType()) {
                        case TEXT -> frontend.showTextMessage(new String(messageBytes), Frontend.Destination.CONSUMER);

                        case IMAGE -> frontend.showImageMessage(contentMessage.filename(), messageBytes);

                        case FILE ->
                                frontend.showFileMessage(contentMessage.filename(), messageBytes, Frontend.Destination.CONSUMER);

                        default -> throw new IllegalStateException("Unexpected message type");
                    }
                });
            } else {
                notifyUser("Failed to process message");
            }
        }

        private void sendMessage(ContentMessage message) {
            if (cipherService != null && outputTopic != null) {
                CompletableFuture<byte[]> encryptedMessageFuture = cipherService.encrypt(message.toBytes());

                encryptedMessageFuture.thenAccept(encryptedMessage -> {
                    KafkaMessage kafkaMessage = new KafkaMessage(KafkaMessage.Action.MESSAGE, encryptedMessage);
                    kafkaWriter.write(kafkaMessage.toBytes(), outputTopic);
                });
            } else {
                notifyUser("Failed to send message");
            }
        }
    }
}
