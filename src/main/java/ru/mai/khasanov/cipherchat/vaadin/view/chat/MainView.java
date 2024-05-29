package ru.mai.khasanov.cipherchat.vaadin.view.chat;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;
import ru.mai.khasanov.cipherchat.model.Room;
import ru.mai.khasanov.cipherchat.model.User;
import ru.mai.khasanov.cipherchat.service.RoomService;
import ru.mai.khasanov.cipherchat.service.ServerService;
import ru.mai.khasanov.cipherchat.service.UserService;
import ru.mai.khasanov.cipherchat.vaadin.Broadcaster;
import ru.mai.khasanov.cipherchat.vaadin.view.login.LoginView;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

@Route(value = "user/:userId")
public class MainView extends HorizontalLayout implements BeforeEnterObserver {
    private User user;

    private final ServerService serverService;
    private final UserService userService;
    private final RoomService roomService;

    private Registration broadcasterRegistration;

    private Frontend frontend;
    private Backend backend;

    public MainView(ServerService serverService, UserService userService, RoomService roomService) {
        this.serverService = serverService;
        this.userService = userService;
        this.roomService = roomService;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        Optional<Long> userIdOptional = beforeEnterEvent.getRouteParameters().getLong("userId");

        if (userIdOptional.isPresent()) {
            Optional<User> userOptional = userService.getUserById(userIdOptional.get());

            if (userOptional.isPresent()) {
                this.user = userOptional.get();

                this.broadcasterRegistration = Broadcaster.register(this::receiveBroadcasterMessage);

                this.backend = new Backend();
                this.frontend = new Frontend();
            }
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
        }
    }

    private void receiveBroadcasterMessage(String message) {
        if (message.equals("update")) {
            updateUI(() -> frontend.updateRoomList());
        }
    }

    private void navigateToChatView(long roomId) {
        UI.getCurrent().navigate(ChatView.class, new RouteParameters(Map.of(
                "userId", String.valueOf(user.getId()),
                "roomId", String.valueOf(roomId)
        )));
    }

    private void navigateToLoginView() {
        UI.getCurrent().navigate(LoginView.class);
    }

    private void notifyUser(String message) {
        updateUI(() -> Notification.show(message));
    }

    private void updateUI(Command command) {
        Optional<UI> maybeUI = getUI();
        maybeUI.ifPresent(ui -> ui.access(command));
    }

    private class Frontend {
        private final Scroller roomsList = new Scroller();

        public Frontend() {
            setSizeFull();
            setSpacing(true);

            Div divider = new Div();
            divider.getStyle().set("width", "1px");
            divider.getStyle().set("background-color", "#394f64");

            add(createLeftPanel(), divider, createRightPanel());
        }

        private VerticalLayout createLeftPanel() {
            VerticalLayout leftPanel = new VerticalLayout();
            leftPanel.setWidth("45%");
            leftPanel.setPadding(false);
            leftPanel.setSpacing(false);

            VerticalLayout userSection = createUserSection();
            VerticalLayout createRoomSection = createRoomSection();

            leftPanel.add(userSection, new Hr(), createRoomSection);
            leftPanel.expand(userSection);

            return leftPanel;
        }

        private VerticalLayout createRightPanel() {
            VerticalLayout rightPanel = new VerticalLayout();
            rightPanel.setWidth("55%");
            rightPanel.setSpacing(false);

            Div roomsTitle = new Div();
            roomsTitle.setWidthFull();
            roomsTitle.add(new H1("Rooms"));
            roomsTitle.getStyle().set("text-align", "center");

            roomsList.setWidthFull();
            roomsList.setContent(createRoomsList());

            rightPanel.add(roomsTitle, new Hr(), roomsList);

            return rightPanel;
        }

        private VerticalLayout createUserSection() {
            VerticalLayout userSection = new VerticalLayout();
            userSection.setWidthFull();
            userSection.setAlignItems(Alignment.CENTER);

            Avatar avatar = new Avatar(user.getUsername());
            avatar.setHeight("200px");
            avatar.setWidth("200px");

            Span username = new Span(user.getUsername());
            username.getStyle().set("font-size", "28px");

            MenuBar menuBar = new MenuBar();
            MenuItem userMenu = menuBar.addItem(username);
            userMenu.getSubMenu().addItem("Sign out", event -> navigateToLoginView());

            userSection.add(avatar, menuBar);
            return userSection;
        }

        private VerticalLayout createRoomSection() {
            VerticalLayout createRoomSection = new VerticalLayout();
            createRoomSection.setWidthFull();
            createRoomSection.setAlignItems(Alignment.CENTER);

            H2 title = new H2("Create new room");

            TextField name = new TextField("Name");

            Select<String> algorithm = new Select<>();
            algorithm.setLabel("Algorithm");
            algorithm.setItems("MARS", "RC6");

            Select<String> mode = new Select<>();
            mode.setLabel("Mode");
            mode.setItems("ECB", "CBC", "CFB", "PCBC", "OFB", "CTR", "RD");

            Select<String> padding = new Select<>();
            padding.setLabel("Padding");
            padding.setItems("Zeros", "PKCS7", "ANSI_X_923", "ISO_10126");

            Button createButton = new Button("Create", event -> {
                if (backend.createRoom(user.getId(), name.getValue(), algorithm.getValue(), mode.getValue(), padding.getValue())) {
                    notifyUser("Room successfully created");
                    Broadcaster.broadcast("update");
                } else {
                    notifyUser("Failed to create room");
                }
            });
            createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            createRoomSection.add(title, name, algorithm, mode, padding, createButton);
            return createRoomSection;
        }

        private HorizontalLayout createRoomComponent(Room room) {
            HorizontalLayout roomComponent = new HorizontalLayout();
            roomComponent.setWidthFull();
            roomComponent.setMargin(true);
            roomComponent.setPadding(true);
            roomComponent.setSpacing(true);
            roomComponent.getStyle()
                    .set("background-color", "#364860")
                    .set("border-radius", "10px");

            StringJoiner roomMembers = new StringJoiner(" ");
            roomMembers.add("Members:");
            for (User user : room.getUsers()) {
                roomMembers.add(user.getUsername());
            }

            Span roomDetails = new Span("Name: " + room.getName() + " | " + roomMembers);

            Button joinButton = new Button("join", event -> {
                if (backend.connectRoom(room.getId())) {
                    notifyUser("You have been successfully connected to the room");
                    Broadcaster.broadcast("update");
                    navigateToChatView(room.getId());
                } else {
                    notifyUser("Failed to connection to the room");
                }
            });

            Button deleteButton = new Button("delete", event -> {
                if (backend.deleteRoom(room.getId())) {
                    notifyUser("Room successfully deleted");
                    Broadcaster.broadcast(String.format("delete_room_%s", room.getId()));
                    Broadcaster.broadcast("update");
                } else {
                    notifyUser("Failed to delete room");
                }
            });

            roomDetails.getStyle().set("font-size", "20px");
            joinButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
            deleteButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

            if (user.getId().equals(room.getOwnerUserId())) {
                roomComponent.add(roomDetails, joinButton, deleteButton);
            } else {
                roomComponent.add(roomDetails, joinButton);
            }

            roomComponent.expand(roomDetails);
            roomComponent.setAlignItems(Alignment.CENTER);

            return roomComponent;
        }

        private VerticalLayout createRoomsList() {
            VerticalLayout roomsList = new VerticalLayout();
            List<Room> rooms = roomService.getAllRooms();
            for (Room room : rooms) {
                if (room.getUsers().size() < 2) {
                    roomsList.add(createRoomComponent(room));
                }
            }

            return roomsList;
        }

        private void updateRoomList() {
            roomsList.setContent(createRoomsList());
        }
    }

    private class Backend {
        private boolean connectRoom(long roomId) {
            return serverService.connectRoom(user.getId(), roomId);
        }

        private boolean deleteRoom(long roomId) {
            return serverService.deleteRoom(roomId);
        }

        private boolean createRoom(long userId, String name, String algorithm, String mode, String padding) {
            return serverService.createRoom(userId, name, algorithm, mode, padding);
        }
    }
}
