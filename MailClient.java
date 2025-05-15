import java.io.*;
import java.net.*;
import java.nio.file.*;

public class MailClient {
    public static void main(String[] args) {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        try {
            String serverAddress = "192.168.0.8"; //다른 컴퓨터에 서버가 있을시 공인 ip로
            int port = 44444;

            Socket socket = new Socket(serverAddress,port);
            socket.setSoTimeout(30000); //시간이 짧으면 조절 가능
            System.out.println("Connected to Mail Server at " + serverAddress + ":" + port);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // 서버 초기 응답
            System.out.println("Server: " + in.readLine());
            boolean isAuthenticated = false; // 인증 상태 추적
            while (true) {

                if (isAuthenticated) {
                    System.out.print("Enter command (e.g., LOGOUT, SEND, LIST, RETR 1, DELE 1, QUIT): ");
                } else {
                    System.out.print("Enter command (e.g., USER user1, SEND, LIST, RETR 1, DELE 1, QUIT): ");
                }
                String command = console.readLine().trim();
                if (command.isEmpty()) continue;

                out.println(command);
                if (command.equalsIgnoreCase("QUIT")) {
                    System.out.println("Server: " + in.readLine());
                    break;
                }

                String response = in.readLine();

                StringBuilder fullResponse = new StringBuilder("Server: " + response + "\n");

                // USER 명령 후 비밀번호 입력 처리
                if (command.toUpperCase().startsWith("USER") && response.equals("Password: ")) {
                    System.out.print("Enter password: ");
                    String password = console.readLine().trim();
                    out.println(password);
                    out.flush();
                    response = in.readLine();
                    if (response == null) {
                        fullResponse.append("Server disconnected unexpectedly.\n");
                    } else {
                        fullResponse.append("Server: ").append(response).append("\n");
                    }
                }

                // 인증 상태 업데이트
                if (command.toUpperCase().startsWith("USER") && response.startsWith("+OK")) {
                    isAuthenticated = true;
                } else if (command.equalsIgnoreCase("LOGOUT") && response.startsWith("+OK")) {
                    isAuthenticated = false;
                }

                // SEND 대화형 입력 처리
                if (command.equalsIgnoreCase("SEND")) {
                    while (response.startsWith("To: ") || response.startsWith("Subject: ") || response.startsWith("Content: ")) {
                        System.out.print(response);
                        String input = console.readLine().trim();
                        out.println(input);
                        out.flush();
                        response = in.readLine();
                        if (response == null) {
                            fullResponse.append("Server disconnected unexpectedly.\n");
                            break;
                        }
                        fullResponse = new StringBuilder(); //send 이후에는 문자열을 출력하지 않기 위함
                    }
                }
// LIST, RETR 다중 라인 처리
                else if (response.startsWith("+OK") && (command.toUpperCase().startsWith("LIST") || command.toUpperCase().startsWith("RETR"))) {
                    if (response.equals("+OK 0 messages")) {
                        // 메시지가 없는 경우 더 이상 읽지 않음
                        fullResponse.append("Server: .\n"); //없어도 문제는 없음
                    } else {
                        while (true) {
                            String line = in.readLine();
                            if (line == null) {
                                fullResponse.append("Server disconnected unexpectedly.\n");
                                break;
                            }
                            fullResponse.append("Server: ").append(line).append("\n");
                            if (line.equals(".")) break;
                        }
                    }
                }

// 모든 응답을 한 번에 출력
                System.out.print(fullResponse.toString());
            }

            out.close();
            in.close();
            socket.close();
            console.close();
            System.out.println("Client disconnected.");

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}