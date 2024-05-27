package ru.mai.khasanov.cipherchat.vaadin.view.chat;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.DetachNotifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.lineawesome.LineAwesomeIcon;
import ru.mai.khasanov.cipherchat.cryptography.DiffieHellmanAlgorithm;
import ru.mai.khasanov.cipherchat.model.Room;
import ru.mai.khasanov.cipherchat.model.User;
import ru.mai.khasanov.cipherchat.service.RoomService;
import ru.mai.khasanov.cipherchat.service.ServerService;
import ru.mai.khasanov.cipherchat.vaadin.Broadcaster;
import ru.mai.khasanov.cipherchat.vaadin.view.login.LoginView;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MainLayout extends AppLayout implements DetachNotifier {
    private final User user;

    private final Registration broadcasterRegistration;

    private final ServerService serverService;
    private final RoomService roomService;

    private final SideNav sideNav = new SideNav();
    private final Dialog dialog = new Dialog();

    public static class RoomTab extends Tab {
        private final RoomInfo roomInfo;

        public RoomTab(RoomInfo roomInfo) {
            super();
            this.roomInfo = roomInfo;
        }
    }

    public static class RoomInfo {
        private final long roomId;
        private final String name;

        public RoomInfo(String name, long roomId) {
            this.roomId = roomId;
            this.name = name;
        }
    }

    public MainLayout(ServerService serverService, RoomService roomService) {
        this.serverService = serverService;
        this.roomService = roomService;

        broadcasterRegistration = Broadcaster.register(this::receiveBroadcasterMessage);

        this.user = VaadinSession.getCurrent().getAttribute(User.class);
        if (user != null) {
            addDrawerContent();
            addHeaderContent();
        } else {
            navigateToLoginView();
        }
    }

    // FRONTEND

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        H2 title = new H2("Cipher chat");

        addToNavbar(toggle, title);
    }

    private void addDrawerContent() {
        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.getStyle()
                .set("padding", "5px")
                .set("border-bottom", "1px solid #394f64");

        buttonBar.setAlignItems(FlexComponent.Alignment.CENTER);
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        Button createButton = new Button("Create", event -> {
            prepareDialogForCreateRoom();
            dialog.open();
        });
        createButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        Button searchButton = new Button("Search", event -> {
            prepareDialogForSearchRoom();
            dialog.open();
        });

        buttonBar.add(createButton, searchButton);

        Header header = new Header(buttonBar);

        fillNavigation();
        Scroller scroller = new Scroller(sideNav);

        addToDrawer(header, scroller, createFooter());
    }

    private void fillNavigation() {
        if (user != null) {
            sideNav.removeAll();

            List<Room> rooms = roomService.getRoomsByUserId(user.getId());

            for (Room room : rooms) {
                sideNav.addItem(new SideNavItem
                        (
                                room.getName(),
                                ChatView.class,
                                new RouteParameters(Map.of(
                                        "userId", String.valueOf(user.getId()),
                                        "roomId", String.valueOf(room.getId())
                                )),
                                LineAwesomeIcon.COMMENTS.create()
                        )
                );
            }
        } else {
            navigateToLoginView();
        }
    }

    private Footer createFooter() {
        Footer footer = new Footer();

        if (user != null) {
            Avatar avatar = new Avatar(user.getUsername());
            avatar.getElement().setAttribute("tabindex", "-1");

            MenuBar userMenu = new MenuBar();
            userMenu.setThemeName("tertiary-inline contrast");

            MenuItem userName = userMenu.addItem("");

            Div div = new Div();
            div.add(avatar);
            div.add(user.getUsername());
            div.add(new Icon("lumo", "dropdown"));

            div.getElement().getStyle()
                    .set("display", "flex")
                    .set("align-items", "center")
                    .set("justify-content", "center")
                    .set("gap", "var(--lumo-space-s)");

            userName.add(div);
            userName.getSubMenu().addItem("Sign out", e -> navigateToLoginView());

            footer.add(userMenu);
        } else {
            navigateToLoginView();
        }

        return footer;
    }

    @SuppressWarnings("unchecked")
    private void prepareDialogForCreateRoom() {
        VerticalLayout content = createCreateDialogContent();

        dialog.removeAll();
        dialog.getHeader().removeAll();
        dialog.getFooter().removeAll();
        dialog.setHeaderTitle("Create room");
        dialog.add(content);

        Button closeButton = new Button(new Icon("lumo", "cross"), event -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.getHeader().add(closeButton);

        Button submitButton = new Button("confirm", event -> {
            String[] information = new String[4];
            information[0] = ((TextField) content.getComponentAt(0)).getValue();

            for (int i = 1; i < content.getComponentCount(); i++) {
                information[i] = ((Select<String>) content.getComponentAt(i)).getValue();
            }

            Room room = createRoom(
                    information[0],
                    information[1],
                    information[2],
                    information[3]
            );

            if (room != null) {
                joinRoom(room.getId());
                updateSideNav();
                dialog.close();
            } else {
                Notification.show("Something went wrong");
                dialog.close();
            }
        });
        submitButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        dialog.getFooter().add(submitButton);
    }

    private void prepareDialogForSearchRoom() {
        VerticalLayout content = createSearchDialogContent();

        dialog.removeAll();
        dialog.getHeader().removeAll();
        dialog.getFooter().removeAll();

        dialog.setHeaderTitle("Search room");
        dialog.add(content);

        Button closeButton = new Button(new Icon("lumo", "cross"), event -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.getHeader().add(closeButton);
    }

    private VerticalLayout createCreateDialogContent() {
        TextField name = new TextField("Room name");

        Select<String> algorithm = new Select<>();
        algorithm.setLabel("Encryption algorithm");
        algorithm.setValue("MARS");
        algorithm.setItems(
                "MARS",
                "RC6"
        );

        Select<String> mode = new Select<>();
        mode.setLabel("Cipher mode");
        mode.setValue("ECB");
        mode.setItems(
                "ECB",
                "CBC",
                "CFB",
                "PCBC",
                "OFB",
                "CTR",
                "RD"
        );

        Select<String> padding = new Select<>();
        padding.setLabel("Encryption algorithm");
        padding.setValue("Zeros");
        padding.setItems(
                "Zeros",
                "PKCS7",
                "ANSI_X_923",
                "ISO_10126"
        );

        VerticalLayout content = new VerticalLayout();
        content.add(
                name,
                algorithm,
                mode,
                padding
        );

        return content;
    }

    private VerticalLayout createSearchDialogContent() {
        List<Room> rooms = roomService.getAllRooms();
        Tabs tabs = new Tabs();

        for (Room room : rooms) {
            tabs.add(createTab(room));
        }

        tabs.setSelectedTab(null);
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.addClassNames(LumoUtility.Flex.GROW, LumoUtility.Flex.SHRINK, LumoUtility.Overflow.HIDDEN);

        tabs.addSelectedChangeListener(event -> {
            RoomInfo roomInfo = ((RoomTab) event.getSelectedTab()).roomInfo;

            ConfirmDialog confirmDialog = new ConfirmDialog();

            confirmDialog.setHeader("Join to chat");
            confirmDialog.setText("Do you want to join the chat?");

            confirmDialog.setCancelable(true);
            confirmDialog.setRejectable(true);

            confirmDialog.setConfirmText("Join");
            confirmDialog.addConfirmListener(e -> {
                joinRoom(roomInfo.roomId);
                updateSideNav();
            });

            confirmDialog.open();
        });

        Scroller scroller = new Scroller(tabs);

        return new VerticalLayout(scroller);
    }

    private RoomTab createTab(Room room) {
        RoomInfo roomInfo = new RoomInfo(room.getName(), room.getId());
        RoomTab tab = new RoomTab(roomInfo);
        tab.addClassNames(LumoUtility.JustifyContent.BETWEEN);
        tab.add(new Span(roomInfo.name));

        return tab;
    }

    // BACKEND

    private Room createRoom(
            String name,
            String algorithm,
            String mode,
            String padding) {

        return serverService.createRoom(name, algorithm, mode, padding);
    }

    private void joinRoom(long roomId) {
        serverService.connectRoom(user.getId(), roomId);
    }

    private void updateSideNav() {
        Optional<UI> maybeUI = getUI();
        if (maybeUI.isPresent()) {
            UI ui = maybeUI.get();
            ui.access(this::fillNavigation);
        }
    }

    private void receiveBroadcasterMessage(String message) {
        if (message.equals("update")) {
            updateSideNav();
        }
    }

    @Override
    public void onDetach(DetachEvent event) {
        broadcasterRegistration.remove();
    }

    private void navigateToLoginView() {
        UI.getCurrent().navigate(LoginView.class);
    }
}
