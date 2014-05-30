package org.synapse.cytoscapeclient.internal;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.JPanel;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.FlowLayout;

public class LoginDialog {
  final JDialog dialog;
  final JTextField userField;
  final JTextField apiKeyField;
  final JLabel failedLabel;
  final JButton okBtn;

  private static JTextArea newHelpArea(final String msg) {
    final JTextArea area = new JTextArea(msg, 2, 40);
    area.setEditable(false);
    area.setLineWrap(true);
    area.setWrapStyleWord(true);
    area.setHighlighter(null);
    area.setOpaque(false);
    final Font font = area.getFont();
    final Font newFont = new Font(font.getName(), font.getStyle(), 10);
    area.setFont(newFont);
    return area;
  }

  public LoginDialog(final Frame parent) {
    dialog = new JDialog(parent, "Sign in to Synapse");

    final JLabel userLabel = new JLabel("Username: ");
    userField = new JTextField();
    userField.getDocument().addDocumentListener(new HideFailedOnChange());
    final JTextArea userHelp = newHelpArea("Your username is located in parenthesis at the top-right corner of the Synapse website next to your full name.");

    final JLabel apiKeyLabel = new JLabel("API Key: ");
    apiKeyField = new JTextField();
    apiKeyField.getDocument().addDocumentListener(new HideFailedOnChange());
    final JTextArea apiKeyHelp = newHelpArea("Your API key can be found by clicking the \"Settings\" icon at the top-right corner of the Synapse website.");

    failedLabel = new JLabel("Sign in failed. Please re-enter your username and API key.");
    failedLabel.setForeground(new Color(0xA82D2D));
    //failedLabel.setVisible(false);

    okBtn = new JButton("Sign in");
    final JButton cancelBtn = new JButton("Cancel");

    final EasyGBC c = new EasyGBC();
    dialog.setLayout(new GridBagLayout());

    dialog.add(userLabel, c.reset().insets(10, 10, 0, 0));
    dialog.add(userField, c.right().expandH().insets(10, 0, 0, 10));
    dialog.add(userHelp, c.down().expandH().spanH(2).insets(4, 10, 0, 10));

    dialog.add(apiKeyLabel, c.down().insets(15, 10, 0, 0));
    dialog.add(apiKeyField, c.right().expandH().insets(10, 0, 0, 10));
    dialog.add(apiKeyHelp, c.down().expandH().spanH(2).insets(4, 10, 0, 10));

    dialog.add(failedLabel, c.down().expandH().spanH(2).insets(20, 10, 0, 10));

    final JPanel btnsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    btnsPanel.add(cancelBtn);
    btnsPanel.add(okBtn);
    dialog.add(btnsPanel, c.down().expandH().spanH(2).insets(15, 0, 10, 10));

    dialog.pack();
    dialog.setVisible(true);
  }

  public static void main(String[] args) {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        new LoginDialog(null);
      }
    });
  }

  class HideFailedOnChange implements DocumentListener {
    public void changedUpdate(DocumentEvent e) {
      hide();
    }

    public void insertUpdate(DocumentEvent e) {
      hide();
    }

    public void removeUpdate(DocumentEvent e) {
      hide();
    }

    private void hide() {
      failedLabel.setVisible(false);
      dialog.pack();
    }
  }
}