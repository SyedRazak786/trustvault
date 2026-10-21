package com.trustvault.assistant;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// The assistant answers from fixed, reviewed replies. It holds no secrets, reads no documents,
// and never changes data. Each topic is matched by its key words, so varied wording still gets an answer.
@RestController
public class AssistantController {

    public record Message(String text) {}
    public record Reply(String answer, List<String> links) {}

    private record Topic(List<String> keywords, String answer, List<String> links) {}

    private static final List<Topic> TOPICS = List.of(
        new Topic(List.of("forgot", "forget", "forgotten", "reset password", "reset my password", "lost password",
                "can't sign in", "cannot sign in", "cant login", "can't login", "password reset", "recover password"),
            "To reset your password, enter your email on the reset page. The reply is the same whether or not the email "
            + "is registered. Support sends you a one-time link, which expires in 30 minutes and works only once. "
            + "I can't reset a password for you, and I never ask for one.",
            List.of("/forgot-password")),

        new Topic(List.of("create account", "create an account", "sign up", "signup", "register", "new account",
                "make account", "open account", "join", "get started"),
            "To create an account, choose Create account, then enter your name, email and a password of at least 12 "
            + "characters. New accounts are owners, so you can upload, share and receive documents.",
            List.of("/signup")),

        new Topic(List.of("sign in", "signin", "log in", "login", "log into", "access my account"),
            "To sign in, enter the email and password you registered with. Your session lasts a short time, so sign in "
            + "again if it ends.",
            List.of("/login")),

        new Topic(List.of("username", "user name", "change my name", "change name", "display name", "change email",
                "change my email", "update email"),
            "You can change your display name and phone number under Profile. Your email address and role can't be "
            + "changed from the profile page yet, so contact support if you need to change your email.",
            List.of("/profile", "/help")),

        new Topic(List.of("profile", "phone number", "update my details", "edit profile", "change password",
                "new password"),
            "Open Profile from the circle in the top right. You can change your display name and phone number there. "
            + "To change your password, enter your current password and then the new one, at least 12 characters.",
            List.of("/profile")),

        new Topic(List.of("share", "sharing", "send a file", "send file", "give access", "send document", "send it to"),
            "Open the document you own, then under Who has access enter the person's email, choose Read or Edit, and "
            + "click Share. The person finds it under Shared with me.",
            List.of("/", "/shared")),

        new Topic(List.of("shared with me", "received", "receive", "i got a file", "someone sent"),
            "Documents other owners have shared with you appear under Shared with me. Open one to download it.",
            List.of("/shared")),

        new Topic(List.of("upload", "add a file", "add document", "attach", "file type", "file size", "10 mb"),
            "Owners can upload a PDF, PNG, JPEG or plain text file up to 10 MB. The file's content decides its type, "
            + "not its name.",
            List.of("/")),

        new Topic(List.of("remove access", "revoke", "stop sharing", "take back", "unshare", "remove someone"),
            "Open the document, find the person under Who has access, and click Remove. Their access stops immediately.",
            List.of("/")),

        new Topic(List.of("delete", "remove document", "trash"),
            "Only the owner can delete a document. Open it and click Delete. It disappears for everyone, and the "
            + "deletion is recorded.",
            List.of("/")),

        new Topic(List.of("download", "open file", "view file", "see my file"),
            "Open a document and click Download. You can download documents you own, and documents shared with you.",
            List.of("/")),

        new Topic(List.of("security", "safe", "secure", "encrypt", "privacy", "protected", "hack"),
            "Every request is checked on the server, passwords are stored only as hashes, and every action is recorded. "
            + "The security page lists what is and is not yet in place.",
            List.of("/security", "/privacy")),

        new Topic(List.of("audit", "activity", "log", "history", "who opened"),
            "Every upload, download, sharing change, sign-in and refused attempt is recorded. Administrators can review "
            + "the activity log and verify that it hasn't been altered.",
            List.of("/security")),

        new Topic(List.of("terms", "rules", "policy", "legal"),
            "The terms of use cover your account, your uploads, and acceptable use. Read the full terms for details.",
            List.of("/terms")),

        new Topic(List.of("contact", "support", "help", "email", "问题", "problem", "issue", "not working", "bug"),
            "You can email support at syedrazak337@gmail.com with your account email and a short description. Please "
            + "don't include your password.",
            List.of("/help", "/contact"))
    );

    private static final String NOT_SUPPORTED =
        "I can't do that from here. I can help with creating an account, signing in, resetting a password, sharing, "
        + "uploading, removing access, deleting, downloads, and security. For anything else, email support.";

    private static final Reply FALLBACK = new Reply(NOT_SUPPORTED, List.of("/help", "/contact"));

    @PostMapping("/api/assistant/chat")
    public Reply chat(@RequestBody Message message) {
        String text = message == null || message.text() == null ? "" : message.text().trim().toLowerCase(Locale.ROOT);

        if (text.isEmpty()) {
            return new Reply("Ask me anything about TrustVault, such as how to share a file, create an account, or reset "
                    + "a password.", List.of("/help"));
        }

        // Score each topic by how many of its keywords appear, and use the strongest match.
        Topic best = null;
        int bestScore = 0;
        for (Topic topic : TOPICS) {
            int score = 0;
            for (String keyword : topic.keywords()) {
                if (text.contains(keyword)) score++;
            }
            if (score > bestScore) {
                bestScore = score;
                best = topic;
            }
        }

        if (best == null) {
            return FALLBACK;
        }
        return new Reply(best.answer(), best.links());
    }
}