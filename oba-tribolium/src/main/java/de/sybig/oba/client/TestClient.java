/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.sybig.oba.client;

import java.net.ConnectException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jdo
 */
public class TestClient {

    public static void main (String[] args){
        TestClient client = new TestClient();
        client.test();
    }

    private void test(){
        try {
            TriboliumConnector connector = new TriboliumConnector();
            connector.setBaseURI("http://localhost:9998");
            OntologyClass root = connector.getRoot();
            System.out.println("root " + root);

            System.out.println("first children " + root.getChildren());
        } catch (ConnectException ex) {
            Logger.getLogger(TestClient.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
