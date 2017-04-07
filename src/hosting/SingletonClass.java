/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hosting;

import static hosting.Hosting.password;
import static hosting.Hosting.url;
import static hosting.Hosting.user;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SingletonClass {
    private static class LazySingletonInitializer {
        static SingletonClass instance = new SingletonClass();
    }

    SingletonClass() {
        try {
            Hosting.con = DriverManager.getConnection(url, user, password);
        } catch (SQLException ex) {
            Logger.getLogger(SingletonClass.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public SingletonClass getInstance() {
        System.out.println("singleton");
        return LazySingletonInitializer.instance;
    }
}