import org.apache.commons.dbcp2.BasicDataSource;

import java.io.*;
import java.net.*;
import java.sql.*;

public class DNSServerInternet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/dnsserver?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "dnshs";
    private static final String DB_PASSWORD = "dnshs";

    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        try (Connection conn = DBCPManager.getConnection()){
            serverSocket = new ServerSocket(44444,50,InetAddress.getByName("0.0.0.0"));
            System.out.println("DNS Server started on port 44444...");
            while (true) {
                Socket clientSocket = null;
                BufferedReader in = null;
                PrintWriter out = null;
                try {
                    // 클라이언트 연결
                    clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getInetAddress());

                    // 입출력 스트림
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    out = new PrintWriter(clientSocket.getOutputStream(), true);

                    // 요청 처리
                    String request = in.readLine();
                    if (request == null) {
                        continue;
                    }
//                    String response = processRequest(request);
//                    out.println(response);
                    String response;
                    do{ // 클라이언트가 연결을 끊을 때까지 반복
                        response = processRequest(request);
                        out.println(response);
                        System.out.println(1);
                    }while ((request = in.readLine()) != null);
                } catch (IOException | SQLException e) {
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
        }
        catch (SQLException e) {
            System.err.println("Server error: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
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

    private static String processRequest(String request) throws SQLException {
        try (Connection conn = DBCPManager.getConnection()) {
            String[] parts = request.split(":", 2);
            if (parts.length < 2) {
                return "Error: Invalid request format";
            }

            String command = parts[0];
            String data = parts[1];

            switch (command) {
                case "N":
                    // 도메인으로 IP 조회
                    PreparedStatement stmt = conn.prepareStatement("SELECT ip FROM DNS WHERE domain = ?");
                    stmt.setString(1, data);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        return "IP: " + rs.getString("ip");
                    }
                    return "Error: Domain not found";

                case "R":
                    // IP로 도메인 조회
                    stmt = conn.prepareStatement("SELECT domain FROM DNS WHERE ip = ?");
                    stmt.setString(1, data);
                    rs = stmt.executeQuery();
                    if (rs.next()) {
                        return "Domain: " + rs.getString("domain");
                    }
                    return "Error: IP not found";
                case "W":
                    // 도메인과 IP 저장
                    String[] writeData = data.split(",");
                    if (writeData.length != 2) {
                        return "Error: Invalid write format. Use 'domain,ip'";
                    }
                    String domain = writeData[0].trim();
                    String ip = writeData[1].trim();

                    // 도메인 중복 체크
                    PreparedStatement checkStmt = conn.prepareStatement("SELECT 1 FROM DNS WHERE domain = ?");
                    checkStmt.setString(1, domain);
                    ResultSet checkRs = checkStmt.executeQuery();
                    if (checkRs.next()) {
                        return "Error: Domain already exists";
                    }

                    // IP 중복 체크
                    checkStmt = conn.prepareStatement("SELECT 1 FROM DNS WHERE ip = ?");
                    checkStmt.setString(1, ip);
                    checkRs = checkStmt.executeQuery();
                    if (checkRs.next()) {
                        return "Error: IP already exists";
                    }

                    // 새 레코드 삽입
                    stmt = conn.prepareStatement("INSERT INTO DNS (domain, ip) VALUES (?, ?)");
                    stmt.setString(1, domain);
                    stmt.setString(2, ip);
                    stmt.executeUpdate();
                    return "Success: Stored " + domain + " -> " + ip;

                default:
                    return "Error: Unknown command";
            }
        } catch (SQLException e) {
            return "Error: Database error - " + e.getMessage();
        }
    }

}

//class DBCPManager {
//    private static BasicDataSource ds = new BasicDataSource();
//
//    static {
//        ds.setUrl("jdbc:mysql://localhost:3306/dnsserver?useSSL=false&allowPublicKeyRetrieval=true&autoReconnect=true");
//        ds.setUsername("dnshs");
//        ds.setPassword("dnshs");
//        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
//
//        // 커넥션 풀 유효성 검사 설정
//        ds.setValidationQuery("SELECT 1");      // 검증 쿼리
//        ds.setTestOnBorrow(true);               // 커넥션 빌릴 때마다 검증
//        ds.setInitialSize(5);
//        ds.setMaxTotal(20);
//        ds.setMaxIdle(10);
//    }
//
//    public static Connection getConnection() throws SQLException {
//        return ds.getConnection();
//    }
//}

