package ru.mai.khasanov.cipherchat.vaadin.view.chat;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import ru.mai.khasanov.cipherchat.model.Room;
import ru.mai.khasanov.cipherchat.model.User;
import ru.mai.khasanov.cipherchat.service.RoomService;
import ru.mai.khasanov.cipherchat.service.ServerService;
import ru.mai.khasanov.cipherchat.vaadin.view.login.LoginView;

import java.util.List;

@Route(value = "user/:userId")
public class MainView extends HorizontalLayout {
    private final User user;

    private final ServerService serverService;
    private final RoomService roomService;

    VerticalLayout menu;
    VerticalLayout userBar;
    VerticalLayout toolBar;
    VerticalLayout chats;

    Tabs tabs = new Tabs();

    private static class RoomTab extends Tab {
        private final RoomInfo roomInfo;

        public RoomTab(RoomInfo roomInfo) {
            super();
            this.roomInfo = roomInfo;
        }
    }

    public record RoomInfo(long roomId, String name) {

    }

    public MainView(ServerService serverService, RoomService roomService) {
        this.serverService = serverService;
        this.roomService = roomService;

        this.user = VaadinSession.getCurrent().getAttribute(User.class);
        if (user != null) {
            this.userBar = createUserBar();
            this.toolBar = createToolBar();
            this.chats = createChats();
            this.menu = createMenu(userBar, toolBar);

            add(menu, chats);
        } else {
            navigateToLoginView();
        }
    }

    private VerticalLayout createMenu(VerticalLayout userBar, VerticalLayout toolBar) {
        return new VerticalLayout(userBar, toolBar);
    }

    private VerticalLayout createChats() {
        List<Room> rooms = roomService.getAllRooms();

        for (Room room : rooms) {
            if (room.getUsers().size() < 2) {
                tabs.add(createTab(room));
            }
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
            confirmDialog.setConfirmText("Join");

            confirmDialog.addConfirmListener(e -> joinRoom(roomInfo.roomId));

            confirmDialog.open();
        });

        Scroller scroller = new Scroller(tabs);

        H1 title = new H1("Rooms");

        return new VerticalLayout(title, scroller);
    }

    private VerticalLayout createUserBar() {
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

        return new VerticalLayout(userMenu);
    }

    private VerticalLayout createToolBar() {
        H2 title = new H2("Create new room");

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

        Button createButton = new Button("Create", event -> createRoom(
                name.getValue(),
                algorithm.getValue(),
                mode.getValue(),
                padding.getValue()
        ));
        createButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        return new VerticalLayout(title, name, algorithm, mode, padding, createButton);
    }

    private RoomTab createTab(Room room) {
        RoomTab tab = new RoomTab(new RoomInfo(room.getId(), room.getName()));
        tab.addClassNames(LumoUtility.JustifyContent.BETWEEN);
        tab.add(new Span(room.getName()));

        return tab;
    }

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

    private void navigateToLoginView() {
        UI.getCurrent().navigate(LoginView.class);
    }

}
