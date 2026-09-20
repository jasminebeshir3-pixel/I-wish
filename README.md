# I-Wish — Fully Integrated Project

Maven project, package `org.example`, now organized by who owns what:

```
org.example
├── MainDashboard.java        Home screen after login — tiles to every screen
├── net/                      Shared: connection helper + logged-in user id
│   ├── ServerClient.java
│   └── Session.java
├── ui/                       Shared design system (colors, fonts, buttons, avatars)
│   └── Ui.java
├── server/                   Person 6 — Server + Database + Integration
│   ├── ServerGUI.java        Start/Stop window (spec item 11)
│   ├── Server.java
│   ├── ClientHandler.java
│   ├── DatabaseManager.java
│   ├── Protocol.java
│   └── TestClient.java
├── person1/                  Person 1 — Authentication
│   └── AuthFrame.java
├── person2/                  Person 2 — Friends System
│   ├── FriendsPanel.java
│   ├── FriendsApi.java
│   ├── RealFriendsApi.java
│   ├── FriendDTO.java
│   ├── FriendRequestDTO.java
│   ├── FriendsException.java
│   └── FriendsDemoApp.java
├── person3/                  Person 3 — My Wish List
│   ├── MyWishListFrame.java
│   ├── AddEditItemDialog.java
│   ├── WishlistClient.java
│   ├── CatalogItem.java
│   ├── WishlistItem.java
│   └── WishlistException.java
├── person4/                  Person 4 — Friends' Wish Lists
│   ├── FriendsWishListsGUI.java
│   ├── FriendWishlistPanel.java
│   └── WishlistItemDetailsGUI.java
└── person5/                  Person 5 — Contributions & Notifications
    ├── ContributionGUI.java
    ├── ContributionsGUI.java
    ├── ContributionProgressGUI.java
    └── NotificationsGUI.java
```

## How to run the whole app
1. Run `sql/schema.sql` in MySQL (creates the database, tables and the sample
   catalog items). DB user/password default to what is in
   `server/DatabaseManager.java`; override them with the `IWISH_DB_USER` /
   `IWISH_DB_PASS` environment variables.
   (Optional) run `sql/reset_test_data.sql` afterwards to load the team's test
   users (password `1234`, everyone is friends) and sample wish list items.
2. Run **`server/ServerGUI.java`** and press **Start Server**
   (or `mvn exec:java`). Headless alternative: `server/Server.java`.
3. Run **`person1/AuthFrame.java`** — this is the app's real starting screen
   (Register/Sign In → Dashboard → every other screen).
   With Maven: `mvn exec:java -Dexec.mainClass=org.example.person1.AuthFrame`.

Every screen still has its own `main()` too, so anyone can keep testing their
own screen standalone (it'll just use `Session.currentUserId`, default `1`,
instead of going through the login screen).

## Who owns which folder
- Whoever is "Person N" only needs to touch their own `personN/` folder.
- `net/`, `ui/`, and `server/` are shared — changes there affect everyone, so
  coordinate before editing them (this is exactly the Protocol.java / net
  package situation from before: everyone's copy needs to match).
- `MainDashboard.java` stays at the root since it's the one file that talks
  to every person's package — think of it as "assembly", not any one
  person's responsibility.
