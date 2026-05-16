package training.web;

import java.util.Map;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@SpringBootApplication
@RestController
public class Main {
    // 宣告 ProductRepository 倉庫介面
    @Autowired
    private ProductRepository productRepository;

    public static void main(String[] args) {
        // 啟動網站應用 http://127.0.0.1:8080/
        SpringApplication.run(Main.class, args);
        // 載入 MySQL Driver / Connector
        try {
            Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @GetMapping("/test-jpa-save")
    public String save(@RequestParam String name) {
        Product p = new Product();
        p.setName(name);
        productRepository.save(p);
        return "已儲存";
    }
    
    // 註冊會員帳號 API
    private boolean insertMember(String name, String email, String password) {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try{
            con = DriverManager.getConnection("jdbc:mysql://localhost/mywebsite?user=root&password=12345678");
            stmt = con.prepareStatement("SELECT * FROM member WHERE email=?");
            stmt.setNString(1, email); 
            rs = stmt.executeQuery();
            if (rs.next()) { // 有取得資料，代表email重複
                return false;
            }
            // 沒有取得資料，才新增一個帳號到資料庫
            stmt = con.prepareStatement("INSERT INTO member(name,email,password) VALUES(?,?,?)");
            stmt.setNString(1, name);
            stmt.setNString(2, email);
            stmt.setNString(3, password);
            stmt.execute();
            return true;  // 沒有跳到錯誤的區塊，那就是成功
        } catch (SQLException e) {
            return false;
        } finally {
            try{
                if(con!=null){
                    con.close();
                }
            } catch (SQLException e) {}
        }
    }
    @PostMapping("/api/member")
    public Map<String, Boolean> signup(@RequestParam String name, @RequestParam String email, @RequestParam String password) {
        boolean result = insertMember(name, email, password);
        return Map.of("ok", result);
    }
    
    // 登入會員帳號 API
    private String getMember(String email, String password) {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = DriverManager.getConnection("jdbc:mysql://localhost/mywebsite?user=root&password=12345678");
            stmt = con.prepareStatement("SELECT * FROM member WHERE email=? AND password=?");
            stmt.setNString(1, email);
            stmt.setNString(2, password);
            rs = stmt.executeQuery();
            if (rs.next()) {  // 有取得資料，代表登入成功，回傳會員的姓名
                return rs.getString("name");
            } else {  // 沒有取得資料，代表登入失敗
                return null;
            }
        } catch (SQLException e) {
            return null;
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {}
        }
    }
    @PutMapping("/api/member/auth")
    public Map<String, Boolean> signin(HttpSession session, @RequestParam String email, @RequestParam String password) {
        String name = getMember(email, password);
        if (name == null) {
            return Map.of("ok", false);
        } else {
            session.setAttribute("member-name", name); // 設定使用者的身份為從資料庫抓取到的姓名
            return Map.of("ok", true);
        }
    }
    
    // 檢查會員登入狀態 API
    @GetMapping("/api/member/auth")
    public Map<String, Object> check(HttpSession session) {
        String name = (String)session.getAttribute("member-name");
        if (name == null) {
            return Map.of("ok", false);
        } else {
            return Map.of("ok", true, "name", name);
        }
    }
}
