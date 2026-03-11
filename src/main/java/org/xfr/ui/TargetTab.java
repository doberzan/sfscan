package org.xfr.ui;

import burp.api.montoya.http.handler.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.xfr.SFScan;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Set;
import java.util.TreeSet;

import static burp.api.montoya.http.handler.RequestToBeSentAction.continueWith;

public class TargetTab extends JPanel implements ActionListener, HttpHandler, MouseListener {
    private SFScan sfScan;

    private JTextArea targetTextArea;

    private JButton clearButton;
    private JButton outputButton;

    private JPopupMenu popupMenu;
    private JMenuItem copyTextItem;
    private JLabel requestLabel;
    private final Set<String> targetSet = new TreeSet<>();

    public TargetTab(SFScan sfScan) {
        this.sfScan = sfScan;
        buildPanelUI();
    }

    private void buildPanelUI() {

        Font font = new Font("SansSerif", Font.BOLD, 16);


        // Build labels
        requestLabel = new JLabel(" Targets (0)");
        requestLabel.setFont(font);

        // Build request text area
        targetTextArea = new JTextArea();
        targetTextArea.setEditable(false);
        targetTextArea.setLineWrap(true);
        targetTextArea.setWrapStyleWord(true);
        addRightClickMenu(targetTextArea);

        // Build buttons
        clearButton = new JButton("Clear");
        outputButton = new JButton("Save Target Data");

        JScrollPane requestAreaScrollPane = new JScrollPane(targetTextArea);

        // Build requests collection
        JPanel requestCollection = new JPanel();
        requestCollection.setLayout(new BorderLayout());
        requestCollection.add(requestLabel, BorderLayout.NORTH);
        requestCollection.add(requestAreaScrollPane, BorderLayout.CENTER);

        // Build button collection
        JPanel buttonCollection = new JPanel();
        buttonCollection.setLayout(new GridLayout(1, 2));
        buttonCollection.add(outputButton);
        buttonCollection.add(clearButton);

        // Add listeners
        clearButton.addActionListener(this);
        outputButton.addActionListener(this);

        // Add items to main panel and set cosmetic preferences
        requestCollection.setBorder(null);
        targetTextArea.setBorder(null);
        clearButton.setPreferredSize(new Dimension(100, 50));

        requestAreaScrollPane.setBorder(null);
        this.setLayout(new BorderLayout());
        this.add(requestCollection, BorderLayout.CENTER);
        this.add(buttonCollection, BorderLayout.SOUTH);

    }

    private void addRightClickMenu(JComponent comp) {
        popupMenu = new JPopupMenu();
        copyTextItem = new JMenuItem("Copy");
        copyTextItem.addActionListener(this);
        popupMenu.add(copyTextItem);
        comp.addMouseListener(this);

    }

    private void updateTargetTextArea()
    {
        synchronized (targetSet) {
            targetTextArea.setText("");
            requestLabel.setText(" Targets ("+targetSet.size()+")");
            for (String s : targetSet)
            {
                targetTextArea.append(s);
            }
        }
    }

    private void clearTargets() {
        targetSet.clear();
    }

    private void saveData() {
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        if (requestToBeSent.isInScope())
        {
            synchronized (targetSet) {
                targetSet.add(requestToBeSent.url().split("\\?")[0] + "\n");
                updateTargetTextArea();
            }
        }
        return continueWith(requestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        return null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == clearButton) {
            clearTargets();
        }
        if (e.getSource() == outputButton) {
            saveData();
        }

        if (e.getSource() == copyTextItem)
        {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(targetTextArea.getSelectedText()), null);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
