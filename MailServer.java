
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class MailServer {
    private static final Map<String, String> users = new HashMap<>();
    private static final String BASE_DIR = "mails/";
    private static final String INBOX_DIR = BASE_DIR + "inbox/";
    private static final String SENT_DIR = BASE_DIR + "sent/";

    // 클라이언트 상태
    private enum State { AUTHORIZATION, TRANSACTION, SEND_MODE }
    private enum SendStep { SEND_TO, SEND_SUBJECT, SEND_CONTENT }

    public static void main(String[] args) {
        // 초기 사용자 설정
        users.put("user1", "password1");
        users.put("user2", "password2");
        // 디렉토리 생성
        new File(INBOX_DIR).mkdirs();
        new File(SENT_DIR).mkdirs();

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(44444, 50, InetAddress.getByName("0.0.0.0"));
            System.out.println("+OK Mail Server started on port 44444");

            while (true) {
                Socket clientSocket = null;
                BufferedReader in = null;
                PrintWriter out = null;
                try {
                    clientSocket = serverSocket.accept();
                    clientSocket.setSoTimeout(30000);
                    System.out.println("Client connected: " + clientSocket.getInetAddress());

                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    out = new PrintWriter(clientSocket.getOutputStream(), true);

                    String currentUser = null;
                    State state = State.AUTHORIZATION;
                    SendStep sendStep = null;
                    String sendTo = null, sendSubject = null, sendContent = null;

                    out.println("+OK Mail Server ready");
                    while (true) {
                        String request = in.readLine();
                        if (request == null) break;

                        if (state == State.SEND_MODE) {
                            switch (sendStep) {
                                case SEND_TO:
                                    sendTo = request.trim();
                                    if (!users.containsKey(sendTo)) {
                                        out.println("-ERR Invalid recipient");
                                        state = State.TRANSACTION;
                                    } else {
                                        out.println("Subject: ");
                                        sendStep = SendStep.SEND_SUBJECT;
                                    }
                                    continue;
                                case SEND_SUBJECT:
                                    sendSubject = request.trim().replaceAll("[^a-zA-Z0-9]", "_"); //오류발생 방지를 위해 숫자와 영어만 가능하게
                                    out.println("Content: ");
                                    sendStep = SendStep.SEND_CONTENT;
                                    continue;
                                case SEND_CONTENT:
                                    sendContent = request.trim();
                                    String response = processSend(currentUser, sendTo, sendSubject, sendContent);
                                    out.println(response);
                                    state = State.TRANSACTION;
                                    sendTo = sendSubject = sendContent = null;
                                    continue;
                            }
                            continue;
                        }

                        String[] parts = request.trim().split("\\s+", 2);
                        String command = parts[0].toUpperCase();
                        String arg = parts.length > 1 ? parts[1] : "";

                        if (command.equals("QUIT")) {
                            out.println("+OK Goodbye");
                            break;
                        }

                        if (state == State.AUTHORIZATION) {
                            if (command.equals("USER")) {
                                if (users.containsKey(arg)) {
                                    currentUser = arg;
                                    out.println("Password: ");
                                    // 비밀번호 입력 대기 상태로 유지
                                    String password = in.readLine();
                                    if (password == null) break;
                                    if (users.get(currentUser).equals(password.trim())) {
                                        state = State.TRANSACTION;
                                        out.println("+OK User accepted");
                                    } else {
                                        currentUser = null;
                                        out.println("-ERR Invalid password");
                                    }
                                } else {
                                    out.println("-ERR Unknown user");
                                }
                            } else {
                                out.println("-ERR Please authenticate with USER");
                            }
                            continue;
                        }

                        switch (command) {
                            case "LOGOUT":
                                currentUser = null;
                                state = State.AUTHORIZATION;
                                out.println("+OK Logged out");
                                break;
                            case "SEND":
                                state = State.SEND_MODE;
                                sendStep = SendStep.SEND_TO;
                                out.println("To: ");
                                break;
                            case "LIST":
                                out.println(processList(currentUser));
                                break;

                            case "DELE":
                                out.println(processDelete(currentUser, arg));
                                break;
                            case "RETR":
                                out.println(processRetrievePOP3(currentUser, arg));
                                break;

//                          IMAP 구현할 때 필요
//                            case "RETRIMAP":
//                                out.println(processRetrieveIMAP(currentUser, arg));
//                                break;
                            default:
                                out.println("-ERR Unknown command");
                        }
                    }
                } catch (SocketTimeoutException e) {
                    System.err.println("Client connection timed out: " + e.getMessage());
                } catch (IOException e) {
                    System.err.println("Error handling client: " + e.getMessage());
                } finally {
                    try {
                        if (out != null) out.close();
                        if (in != null) in.close();
                        if (clientSocket != null) clientSocket.close();
                    } catch (IOException e) {
                        System.err.println("Error closing client resources: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                    System.out.println("Server socket closed.");
                }
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }
    }

    private static String processSend(String sender, String to, String subject, String content) {
        try {
            String filename = "from_" + sender + "_to_" + to + "_" + subject + ".txt";

            Path sentPath = Paths.get(SENT_DIR, sender, filename);

            Files.createDirectories(sentPath.getParent());
            Files.writeString(sentPath, content);

            return "+OK Mail sent and stored";
        } catch (IOException e) {
            return "-ERR Error sending mail: " + e.getMessage();
        }
    }

    private static String processList(String user) {
        File userDir = new File(INBOX_DIR + user);
        if (!userDir.exists()) {
            return "+OK 0 messages";
        }
        File[] mails = userDir.listFiles((dir, name) -> name.endsWith(".txt"));
        if (mails == null || mails.length == 0) {
            return "+OK 0 messages";
        }
        StringBuilder response = new StringBuilder("+OK " + mails.length + " messages\n");
        for (int i = 0; i < mails.length; i++) {
            String name = mails[i].getName();
            String[] parts = name.replace(".txt", "").split("_");
            response.append((i + 1)).append(" from=").append(parts[1])
                    .append(" to=").append(parts[3]).append(" subject=").append(parts[4])
                    .append(" size=").append(mails[i].length()).append("\n");
        }
        response.append(".");
        return response.toString();
    }

    private static String processRetrieveIMAP(String user, String id) {
        try {
            File userDir = new File(INBOX_DIR + user);
            File[] mails = userDir.listFiles((dir, name) -> name.endsWith(".txt"));
            if (mails == null || Integer.parseInt(id) < 1 || Integer.parseInt(id) > mails.length) {
                return "-ERR No such message";
            }
            Path mailPath = mails[Integer.parseInt(id) - 1].toPath();
            String content = Files.readString(mailPath);
            String name = mailPath.getFileName().toString().replace(".txt", "");
            String[] parts = name.split("_", 4);
            return "+OK message follows\nFrom: " + parts[1] + "\nTo: " + parts[3] +
                    "\nSubject: " + parts[4] + "\n\n" + content + "\n.";
        } catch (IOException | NumberFormatException e) {
            return "-ERR Error reading message: " + e.getMessage();
        }
    }

    private static String processDelete(String user, String id) {
        try {
            File userDir = new File(INBOX_DIR + user);
            File[] mails = userDir.listFiles((dir, name) -> name.endsWith(".txt"));
            if (mails == null || Integer.parseInt(id) < 1 || Integer.parseInt(id) > mails.length) {
                return "-ERR No such message";
            }
            Files.delete(mails[Integer.parseInt(id) - 1].toPath());
            return "+OK Message deleted";
        } catch (IOException | NumberFormatException e) {
            return "-ERR Error deleting message: " + e.getMessage();
        }
    }

    private static String processRetrievePOP3(String user, String id) {
        try {
            File userDir = new File(INBOX_DIR + user);
            File[] mails = userDir.listFiles((dir, name) -> name.endsWith(".txt"));
            if (mails == null || Integer.parseInt(id) < 1 || Integer.parseInt(id) > mails.length) {
                return "-ERR No such message";
            }
            Path mailPath = mails[Integer.parseInt(id) - 1].toPath();
            String content = Files.readString(mailPath);
            String name = mailPath.getFileName().toString();
            String[] parts = name.replace(".txt", "").split("_");

            // 다운로드 폴더에 저장
            Path downloadPath = Paths.get("downloads", name);
            Files.createDirectories(downloadPath.getParent());
            Files.writeString(downloadPath, "From: " + parts[1] + "\nTo: " + parts[3] +
                    "\nSubject: " + parts[4] + "\n\n" + content);

            // 서버에서 메일 삭제
            Files.delete(mailPath);

            // 다운로드한 파일 내용 반환`
            return "+OK Downloaded and deleted, content follows\nFrom: " + parts[1] +
                    "\nTo: " + parts[3] + "\nSubject: " + parts[4] + "\n\n" + content + "\n.";
        } catch (IOException | NumberFormatException e) {
            return "-ERR Error downloading message: " + e.getMessage();
        }
    }
}