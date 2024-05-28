//package ru.mai.khasanov.cipherchat.vaadin.view.chat;
//
//import com.vaadin.flow.component.DetachEvent;
//import com.vaadin.flow.component.UI;
//import com.vaadin.flow.component.button.Button;
//import com.vaadin.flow.component.button.ButtonVariant;
//import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
//import com.vaadin.flow.component.contextmenu.MenuItem;
//import com.vaadin.flow.component.contextmenu.SubMenu;
//import com.vaadin.flow.component.html.*;
//import com.vaadin.flow.component.icon.VaadinIcon;
//import com.vaadin.flow.component.menubar.MenuBar;
//import com.vaadin.flow.component.messages.MessageInput;
//import com.vaadin.flow.component.notification.Notification;
//import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
//import com.vaadin.flow.component.orderedlayout.VerticalLayout;
//import com.vaadin.flow.component.upload.Upload;
//import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
//import com.vaadin.flow.router.*;
//import com.vaadin.flow.server.StreamResource;
//import com.vaadin.flow.theme.lumo.LumoUtility;
//import org.apache.kafka.clients.consumer.ConsumerConfig;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.apache.kafka.clients.consumer.ConsumerRecords;
//import org.apache.kafka.clients.consumer.KafkaConsumer;
//import org.apache.kafka.common.serialization.ByteArrayDeserializer;
//import ru.mai.khasanov.cipherchat.cryptography.DiffieHellmanAlgorithm;
//import ru.mai.khasanov.cipherchat.kafka.KafkaWriter;
//import ru.mai.khasanov.cipherchat.model.Room;
//import ru.mai.khasanov.cipherchat.model.RoomCipherInfo;
//import ru.mai.khasanov.cipherchat.model.User;
//import ru.mai.khasanov.cipherchat.model.message.ExchangeKeyMessage;
//import ru.mai.khasanov.cipherchat.model.message.KafkaMessage;
//import ru.mai.khasanov.cipherchat.model.message.DestinationMessage;
//import ru.mai.khasanov.cipherchat.service.RoomService;
//import ru.mai.khasanov.cipherchat.service.ServerService;
//import ru.mai.khasanov.cipherchat.service.UserService;
//import ru.mai.khasanov.cipherchat.vaadin.Broadcaster;
//import ru.mai.khasanov.cipherchat.vaadin.view.login.LoginView;
//
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.time.Duration;
//import java.time.LocalTime;
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//@PageTitle("Chat")
//@Route(value = "user/:userId/room/:roomId?", layout = MainLayout.class)
//public class ChatView extends HorizontalLayout implements BeforeEnterObserver {
//    private User user;
//    private Room room;
//
//    private byte[] ownPrivateKey;
//    private byte[] sharedPrivateKey;
//
//    private final ServerService serverService;
//    private final UserService userService;
//    private final RoomService roomService;
//
//    private final KafkaWriter kafkaWriter;
//    private KafkaConsumer<byte[], byte[]> kafkaConsumer;
//    private List<String> subscribedTopics = new ArrayList<>();
//    private volatile boolean running;
//
//    private ExecutorService executorService;
//
//    private final Map<Long, VerticalLayout> roomContainers;
//
//    public ChatView(ServerService serverService, UserService userService, RoomService roomService, KafkaWriter kafkaWriter) {
//        this.serverService = serverService;
//        this.userService = userService;
//        this.roomService = roomService;
//
//        this.kafkaWriter = kafkaWriter;
//
//        this.roomContainers = new HashMap<>();
//
//        addClassNames("chat-view", LumoUtility.Width.FULL, LumoUtility.Display.FLEX, LumoUtility.Flex.AUTO);
//        setSpacing(false);
//
//        setSizeFull();
//
//        startKafkaConsumer();
//    }
//
//    @Override
//    public void beforeEnter(BeforeEnterEvent event) {
//        Optional<Long> userIdOptional = event.getRouteParameters().getLong("userId");
//        Optional<Long> roomIdOptional = event.getRouteParameters().getLong("roomId");
//
//        Runnable toLoginPage = this::navigateToLoginView;
//
//        if (userIdOptional.isEmpty()) {
//            navigateToLoginView();
//        } else {
//            Optional<User> maybeUser = userService.getUserById(userIdOptional.get());
//            maybeUser.ifPresentOrElse(user -> this.user = user, toLoginPage);
//
//            if (roomIdOptional.isPresent()) {
//                Optional<Room> maybeRoom = roomService.getRoomById(roomIdOptional.get());
//                maybeRoom.ifPresentOrElse(room -> this.room = room, toLoginPage);
//            }
//        }
//
//        loadContent();
//    }
//
//    @Override
//    protected void onDetach(DetachEvent detachEvent) {
//        super.onDetach(detachEvent);
//        stopKafkaConsumer();
//        kafkaWriter.close();
//    }
//
//    // FRONTEND
//
//    private void loadContent() {
//        removeAll();
//
//        long roomId = this.room == null ? 0 : this.room.getId();
//        VerticalLayout roomContent;
//        if (roomContainers.containsKey(roomId)) {
//            roomContent = roomContainers.get(roomId);
//        } else {
//            roomContent = roomId == 0 ? createVoidPage() : createRoomPage();
//            roomContainers.put(roomId, roomContent);
//        }
//
//        add(roomContent);
//    }
//
//    private VerticalLayout createVoidPage() {
//        H1 text = new H1("Choose a room");
//
//        VerticalLayout content = new VerticalLayout();
//        content.setAlignItems(Alignment.CENTER);
//        content.setJustifyContentMode(JustifyContentMode.CENTER);
//        content.add(text);
//
//        return content;
//    }
//
//    private VerticalLayout createRoomPage() {
//        VerticalLayout roomContainer = new VerticalLayout();
//
//        HorizontalLayout header = createRoomHeaderContent();
//        VerticalLayout messageContainer = createRoomMessageContent();
//        HorizontalLayout inputContainer = createRoomInputContent();
//
//        roomContainer.add(header, messageContainer, inputContainer);
//        roomContainer.expand(messageContainer);
//
//        return roomContainer;
//    }
//
//    private HorizontalLayout createRoomHeaderContent() {
//        H2 roomName = new H2(room.getName());
//
//        MenuBar menuBar = new MenuBar();
//        menuBar.setOpenOnHover(true);
//        MenuItem members = menuBar.addItem(VaadinIcon.USERS.create());
//
//        Set<User> users = room.getUsers();
//        if (users != null && !users.isEmpty()) {
//            SubMenu subMenu = members.getSubMenu();
//            for (User user : users) {
//                subMenu.addItem(user.getUsername());
//            }
//        }
//
//        Button disconnectButton = new Button("Disconnect", event -> {
//            ConfirmDialog confirmDialog = new ConfirmDialog();
//
//            confirmDialog.setHeader("Disconnect from room");
//            confirmDialog.setText("Do you want to disconnect from this room?");
//
//            confirmDialog.setCancelable(true);
//            confirmDialog.setRejectable(true);
//            confirmDialog.setConfirmText("Disconnect");
//            confirmDialog.addConfirmListener(e -> disconnectRoom());
//
//            confirmDialog.open();
//        });
//        disconnectButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
//
//        Button deleteButton = new Button("Delete", event -> {
//            ConfirmDialog confirmDialog = new ConfirmDialog();
//
//            confirmDialog.setHeader("Delete room");
//            confirmDialog.setText("Do you want to delete this room?");
//
//            confirmDialog.setCancelable(true);
//            confirmDialog.setRejectable(true);
//            confirmDialog.setConfirmText("Delete");
//            confirmDialog.addConfirmListener(e -> deleteRoom());
//
//            confirmDialog.open();
//        });
//        deleteButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
//
//        HorizontalLayout header = new HorizontalLayout();
//
//        header.setWidthFull();
//        header.setAlignItems(Alignment.CENTER);
//        header.getStyle().set("border-bottom", "1px solid #394f64");
//
//        header.add(roomName, menuBar, disconnectButton, deleteButton);
//        header.expand(menuBar);
//
//        return header;
//    }
//
//    private VerticalLayout createRoomMessageContent() {
//        VerticalLayout messageContainer = new VerticalLayout();
//        messageContainer.getStyle().set("overflow-y", "auto");
//        messageContainer.setSizeFull();
//
//        return messageContainer;
//    }
//
//    private HorizontalLayout createRoomInputContent() {
//        MessageInput messageInput = new MessageInput();
//        Upload upload = createUpload();
//
//        messageInput.getElement().getStyle().set("overflow-y", "auto");
//
//        messageInput.addSubmitListener(event -> {
//            upload.getElement().callJsFunction("uploadFiles");
//            showTextMessage(event.getValue());
//        });
//
//        HorizontalLayout horizontalLayout = new HorizontalLayout();
//        horizontalLayout.setWidthFull();
//        horizontalLayout.addAndExpand(messageInput);
//
//        HorizontalLayout inputContainer = new HorizontalLayout();
//        inputContainer.setAlignItems(Alignment.STRETCH);
//        inputContainer.setHeight("100px");
//
//        inputContainer.setWidthFull();
//        inputContainer.add(horizontalLayout, upload);
//        inputContainer.expand(horizontalLayout);
//
//        return inputContainer;
//    }
//
//    private Upload createUpload() {
//        MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
//        Upload upload = new Upload(buffer);
//
//        upload.setAutoUpload(false);
//        upload.setDropAllowed(false);
//
//        upload.addSucceededListener(event -> {
//            String filename = event.getFileName();
//            String type = event.getMIMEType();
//            sendFileMessage(filename, type, buffer.getInputStream(filename));
//        });
//
//        return upload;
//    }
//
//    // BACKEND
//
//    private void deleteRoom() {
//        serverService.deleteRoom(room.getId());
//        navigateToMainPage();
//        Broadcaster.broadcast("update");
//    }
//
//    private void disconnectRoom() {
//        serverService.disconnectRoom(user.getId(), room.getId());
//        navigateToMainPage();
//        Broadcaster.broadcast("update");
//    }
//
//    private void sendTextMessage(String message) {
//    }
//
//    private void sendFileMessage(String filename, String type, InputStream inputStream) {
//        try {
//            byte[] data = readInputSteam(inputStream);
//            if (isImage(type)) {
//                showImageMessage(filename, data);
//            } else {
//                showFileMessage(filename, data);
//            }
//        } catch (IOException e) {
//            Notification.show("Something wrong");
//        }
//    }
//
//    private void showTextMessage(String message) {
//        Optional<UI> maybeUI = getUI();
//
//        if (maybeUI.isPresent()) {
//            UI ui = maybeUI.get();
//            ui.access(() -> {
//                HorizontalLayout messageContent = new HorizontalLayout();
//                messageContent.getStyle()
//                        .set("display", "flex")
//                        .set("align-items", "flex-end");
//                messageContent.setSpacing(true);
//
//                Span messageSpan = new Span(message);
//                messageSpan.getStyle()
//                        .set("font-size", "15px");
//
//                Span timeSpan = new Span(getCurrentTime());
//                timeSpan.getStyle()
//                        .set("font-size", "10px")
//                        .set("color", "#888")
//                        .set("margin-left", "7px");
//
//                messageContent.add(messageSpan, timeSpan);
//
//
//                // синий #2b5377
//                // темный #0e1621
//
//                Div div = new Div();
//
//                div.getStyle()
//                        .set("border-radius", "12px")
//                        .set("padding", "5px")
//                        .set("background-color", "#2b5377");
//
//                div.add(messageContent);
//
//                VerticalLayout roomContainer = roomContainers.get(room.getId());
//                VerticalLayout messageContainer = (VerticalLayout) roomContainer.getComponentAt(1);
//                messageContainer.add(div);
//            });
//        }
//    }
//
//    private void showImageMessage(String filename, byte[] imageData) {
//        Optional<UI> maybeUI = getUI();
//
//        if (maybeUI.isPresent()) {
//            UI ui = maybeUI.get();
//            ui.access(() -> {
//                Div div = new Div();
//
//                StreamResource imageResource = new StreamResource(filename, () -> new ByteArrayInputStream(imageData));
//                Image image = new Image(imageResource, "image");
//
//                image.setWidth("420px");
//                image.setHeight("360px");
//                image.getStyle()
//                        .set("border", "1px solid #ccc")
//                        .set("border-radius", "25px");
//
//                div.add(image);
//
//                VerticalLayout roomContainer = roomContainers.get(room.getId());
//                VerticalLayout messageContainer = (VerticalLayout) roomContainer.getComponentAt(1);
//                messageContainer.add(div);
//            });
//        }
//    }
//
//    private void showFileMessage(String filename, byte[] fileData) {
//        Optional<UI> maybeUI = getUI();
//
//        if (maybeUI.isPresent()) {
//            UI ui = maybeUI.get();
//            ui.access(() -> {
//
//            });
//        }
//    }
//
//
//    private String getCurrentTime() {
//        LocalTime currentTime = LocalTime.now();
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
//        return currentTime.format(formatter);
//    }
//
//    private boolean isImage(String type) {
//        return type.startsWith("image/");
//    }
//
//    private void navigateToMainPage() {
//        UI.getCurrent().navigate(ChatView.class, new RouteParameters("userId", String.valueOf(user.getId())));
//    }
//
//    private void navigateToLoginView() {
//        UI.getCurrent().navigate(LoginView.class);
//    }
//
//    private void startKafkaConsumer() {
//        kafkaConsumer = new KafkaConsumer<>(Map.of(
//                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9093",
//                ConsumerConfig.GROUP_ID_CONFIG, "chat-group",
//                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
//        ), new ByteArrayDeserializer(), new ByteArrayDeserializer());
//        updateSubscription();
//
//        System.out.println("Kafka started");
//
//        running = true;
//        executorService = Executors.newSingleThreadExecutor();
//        executorService.submit(() -> {
//            while (running) {
//                ConsumerRecords<byte[], byte[]> records = kafkaConsumer.poll(Duration.ofMillis(100));
//                for (ConsumerRecord<byte[], byte[]> record : records) {
//                    System.out.println(record);
//                    handleKafkaMessage(record);
//                }
//            }
//        });
//    }
//
//    private void handleKafkaMessage(ConsumerRecord<byte[], byte[]> record) {
//        KafkaMessage message = KafkaMessage.toKafkaMessage(new String(record.value()));
//        String topic = record.topic();
//        String roomId = topic.split("_")[2];
//
//        System.out.println(message.action());
//
//        // Обработка сообщений Kafka
//        switch (message.action()) {
//            case SETUP_CONNECTION -> {
//                if (message.content() instanceof DestinationMessage destinationMessage) {
//                    exchangePublicKey(destinationMessage);
//                } else {
//                    Notification.show("create_private_key");
//                }
//            }
//
//            case EXCHANGE_PUBLIC_KEY -> {
//                if (message.content() instanceof ExchangeKeyMessage keyMessage) {
//                    setSharedPrivateKey(keyMessage);
//                } else {
//                    Notification.show("exchange_public_key");
//                }
//            }
//
//            case SEND_MESSAGE -> {
//                // Обработка отправленного сообщения
//            }
//
//            default -> throw new IllegalStateException("Unexpected type");
//        }
//    }
//
//    private void exchangePublicKey(DestinationMessage destinationMessage) {
//        RoomCipherInfo roomCipherInfo = room.getRoomCipherInfo();
//
//        long thisUserId = destinationMessage.consumerId();
//        long anotherUserId = destinationMessage.producerId();
//
//        if (thisUserId != this.user.getId()) {
//            return;
//        }
//
//
//        String topic = String.format("input_room_%s", room.getId());
//
//        ownPrivateKey = DiffieHellmanAlgorithm.generateOwnPrivateKey();
//        byte[] ownPublicKey = DiffieHellmanAlgorithm.generateOwnPublicKey(
//                ownPrivateKey,
//                roomCipherInfo.getG(),
//                roomCipherInfo.getP());
//
//        DestinationMessage thisUserMessage = new DestinationMessage(thisUserId, anotherUserId);
//
//        ExchangeKeyMessage message = new ExchangeKeyMessage(thisUserMessage, ownPublicKey);
//        KafkaMessage keyMessage = new KafkaMessage(KafkaMessage.Action.EXCHANGE_PUBLIC_KEY, message);
//
//        kafkaWriter.write(keyMessage.toBytes(), topic);
//    }
//
//    private void setSharedPrivateKey(ExchangeKeyMessage keyMessage) {
//        RoomCipherInfo roomCipherInfo = room.getRoomCipherInfo();
//        DestinationMessage destinationMessage = keyMessage.destinationMessage();
//
//        long thisUserId = destinationMessage.consumerId();
//        long anotherUserId = destinationMessage.producerId();
//
//        if (thisUserId != this.user.getId()) {
//            return;
//        }
//
//        sharedPrivateKey = DiffieHellmanAlgorithm.calculateSharedPrivateKey(
//                keyMessage.publicKey(),
//                ownPrivateKey,
//                roomCipherInfo.getP());
//
//        System.out.println(Arrays.toString(sharedPrivateKey));
//    }
//
//    private void updateSubscription() {
//        subscribedTopics = getAllRoomTopics();
//        kafkaConsumer.subscribe(subscribedTopics);
//    }
//
//    private List<String> getAllRoomTopics() {
//        List<Room> rooms = roomService.getAllRooms();
//        List<String> topics = new ArrayList<>();
//        for (Room room : rooms) {
//            topics.add("input_room_" + room.getId());
//        }
//        return topics;
//    }
//
//    public void addRoomTopic(String roomId) {
//        String newTopic = "input_room_" + roomId;
//        if (!subscribedTopics.contains(newTopic)) {
//            subscribedTopics.add(newTopic);
//            updateSubscription();
//        }
//    }
//
//    private void stopKafkaConsumer() {
//        if (kafkaConsumer != null) {
//            running = false;
//            kafkaConsumer.wakeup();
//            executorService.shutdown();
//            kafkaConsumer.close();
//        }
//    }
//}