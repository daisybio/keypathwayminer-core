package dk.sdu.kpm.utils;

import javax.swing.*;

/**
 * Created by Martin on 25-02-2015.
 */
public class DialogUtils {
    public static void showNonModalDialog(Object message, String title, MessageTypes messageType){

        int messageTypeInt = JOptionPane.ERROR_MESSAGE;
        if(messageType == MessageTypes.Info){
            messageTypeInt = JOptionPane.INFORMATION_MESSAGE;
        }
        if(messageType == MessageTypes.Warn){
            messageTypeInt = JOptionPane.WARNING_MESSAGE;
        }

        JOptionPane pane = new JOptionPane(message, messageTypeInt);
        // Configure via set methods
        JDialog dialog = pane.createDialog(null, title);
        dialog.setModal(false); // this says not to block background components
        dialog.pack();
        dialog.setVisible(true);
        dialog.requestFocus();
    }

    public enum MessageTypes{
        Error, Info, Warn
    }
}
