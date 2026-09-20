
package org.example;
public class Protocol {

    // Field / record separators for list data
    public static final String FIELD_SEP  = ",,";
    public static final String RECORD_SEP = ";;";
    public static final String ARG_SEP    = "\\|"; // regex split on '|'

    // Generic response prefixes
    public static final String OK    = "OK";
    public static final String ERROR = "ERROR";

    // Person 1: Authentication
    public static final String REGISTER = "REGISTER";           // REGISTER|username|password|email
    public static final String LOGIN    = "LOGIN";               // LOGIN|username|password

    // Person 2: Friends system
    public static final String ADD_FRIEND        = "ADD_FRIEND";         // ADD_FRIEND|userId|friendUsername
    public static final String REMOVE_FRIEND      = "REMOVE_FRIEND";      // REMOVE_FRIEND|userId|friendId
    public static final String ACCEPT_FRIEND      = "ACCEPT_FRIEND";      // ACCEPT_FRIEND|requestId
    public static final String DECLINE_FRIEND     = "DECLINE_FRIEND";     // DECLINE_FRIEND|requestId
    public static final String GET_FRIEND_REQUESTS = "GET_FRIEND_REQUESTS"; // GET_FRIEND_REQUESTS|userId
    public static final String GET_SENT_FRIEND_REQUESTS = "GET_SENT_FRIEND_REQUESTS"; // GET_SENT_FRIEND_REQUESTS|userId
    public static final String CANCEL_FRIEND_REQUEST = "CANCEL_FRIEND_REQUEST"; // CANCEL_FRIEND_REQUEST|requestId
    public static final String GET_FRIENDS        = "GET_FRIENDS";        // GET_FRIENDS|userId

    // Person 3: My wish list
    public static final String GET_AVAILABLE_ITEMS  = "GET_AVAILABLE_ITEMS";  // GET_AVAILABLE_ITEMS
    public static final String CREATE_WISHLIST_ITEM = "CREATE_WISHLIST_ITEM"; // CREATE_WISHLIST_ITEM|userId|itemId|price
    public static final String UPDATE_WISHLIST_ITEM = "UPDATE_WISHLIST_ITEM"; // UPDATE_WISHLIST_ITEM|wishlistItemId|newPrice
    public static final String DELETE_WISHLIST_ITEM = "DELETE_WISHLIST_ITEM"; // DELETE_WISHLIST_ITEM|wishlistItemId
    public static final String GET_MY_WISHLIST      = "GET_MY_WISHLIST";      // GET_MY_WISHLIST|userId

    //  Person 4: Friends' wish lists ----------
    // NOTE (Person 6, fix): now requires the caller's own id too, so the server can
    // check they're actually friends before returning someone else's wish list.
    public static final String GET_FRIEND_WISHLIST = "GET_FRIEND_WISHLIST"; // GET_FRIEND_WISHLIST|myUserId|friendUserId

    // Person 5: Contributions & notifications
    public static final String CONTRIBUTE        = "CONTRIBUTE";        // CONTRIBUTE|contributorUserId|wishlistItemId|amount
    public static final String GET_NOTIFICATIONS = "GET_NOTIFICATIONS"; // GET_NOTIFICATIONS|userId
    public static final String MARK_NOTIFICATION_READ = "MARK_NOTIFICATION_READ"; // MARK_NOTIFICATION_READ|notificationId
}
