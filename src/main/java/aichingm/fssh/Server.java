package aichingm.fssh;

import aichingm.fssh.database.Login;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Time;
import java.util.Scanner;

public class Server {

    public static void main(String[] args) throws Exception {

        // this uses h2 by default but change to match your database
        String databaseUrl = "jdbc:sqlite:login.db";
        // create a connection source to our database
        ConnectionSource connectionSource = new JdbcConnectionSource(databaseUrl);

        // instantiate the dao
        final Dao<Login, String> accountDao = DaoManager.createDao(connectionSource, Login.class);

        // if you need to create the 'login' table make this call
        TableUtils.createTableIfNotExists(connectionSource, Login.class);




        SshServer sshd = SshServer.setUpDefaultServer();
        sshd.setPort(2222);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser")));
        sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
            public boolean authenticate(String username, String password, ServerSession session) throws PasswordChangeRequiredException {
                Login login = new Login();
                login.setName(username);
                login.setPassword(password);
                login.setTimestamp(System.currentTimeMillis());
                try {
                    accountDao.create(login);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return false;
            }
        });
        try {
            sshd.start();
            System.out.println("hit enter to exit");
            new Scanner(System.in).nextLine();
            sshd.stop();
        connectionSource.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
