package org.xfr.ui;

import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.params.HttpParameterType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.xfr.SFScan;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static burp.api.montoya.http.handler.RequestToBeSentAction.continueWith;

public class SFIdTab extends JPanel implements ActionListener, HttpHandler, MouseListener {
    private final SFScan sfScan;
    private JButton saveButton;
    private JTextArea textArea;
    private JPopupMenu popupMenu;
    private JMenuItem copyTextItem;
    private JLabel idLabel;

    // TreeSet enforces uniqueness and order
    private final TreeSet<String> sfIds;

    public SFIdTab(SFScan sfScan) {
        this.sfScan = sfScan;
        this.sfIds = new TreeSet<>();

        loadPersistentData();
        buildPanelUI();
    }

    private void buildPanelUI() {
        this.setLayout(new BorderLayout());


        saveButton = new JButton("Save Ids");
        JPanel buttonCollection = new JPanel();
        buttonCollection.setLayout(new GridLayout(1, 3));
        saveButton.setPreferredSize(new Dimension(100, 50));

        Font font = new Font("SansSerif", Font.BOLD, 16);

        // Build labels
        idLabel = new JLabel(" SFIDs (0)");
        idLabel.setFont(font);

        // Build textarea
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        addRightClickMenu(textArea);

        // Add listeners
        saveButton.addActionListener(this);

        // Add items to panel
        buttonCollection.add(saveButton);
        this.add(new JScrollPane(textArea));
        this.add(idLabel, BorderLayout.NORTH);
        this.add(buttonCollection, BorderLayout.SOUTH);
        updateIdTextArea();
    }

    private void saveData() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("sfid-list.txt"));

        if (fileChooser.showSaveDialog(this.getParent()) == JFileChooser.APPROVE_OPTION) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileChooser.getSelectedFile().getAbsolutePath()))) {
                for (String s : sfIds.toArray(new String[0]))
                {
                    writer.write(s + "\n");
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    // Used to do bulk decoding of HTML
    public static String safeDecode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return URLDecoder.decode(
                    s.replaceAll("%(?![0-9a-fA-F]{2})", "%25"), StandardCharsets.UTF_8);
        }
    }

    private void addSFID(String id) {
        String prefix = id.substring(0, 3);

        // If id has checksum remove it
        if(id.length() == 18)
        {
            id = id.substring(0,15);
        }

        // Check if ID is duplicate
        if(sfIds.contains(id))
        {
            return;
        }
        sfIds.add(id);

        updateIdTextArea();
    }

    private void checkSFID(String content)
    {
        // SF id pattern (yikes I know there is probably a better way)
        Pattern pattern = Pattern.compile("\\b[a-zA-Z0-9]{5}0[a-zA-Z0-9]{9}(?:[a-zA-Z0-9]{3})?\\b");
        Matcher matcher = pattern.matcher(content);
        int ctr = 0;
        while (matcher.find()) {
            if (ctr > 1000)
            {
                break;
            }
            ctr++;
            addSFID(matcher.group().strip());
        }
    }

    private void addRightClickMenu(JComponent comp) {
        popupMenu = new JPopupMenu();
        copyTextItem = new JMenuItem("Copy");
        copyTextItem.addActionListener(this);
        popupMenu.add(copyTextItem);
        comp.addMouseListener(this);
    }

    private void updateIdTextArea()
    {
        synchronized (sfIds) {
            textArea.setText("");
            idLabel.setText(" SFIDs ("+sfIds.size()+")");
            for (String s : sfIds)
            {
                textArea.append(s+'\n');
            }
        }
    }

    public void loadPersistentData()
    {
        ObjectMapper mapper = new ObjectMapper();
        String jsonData = sfScan.api.persistence().extensionData().getString("sfscan_ids");

        if (jsonData == null || jsonData.isBlank()) {
            return;
        }

        try {
            JsonNode targetsRoot = mapper.readTree(jsonData);
            for(String s: mapper.convertValue(targetsRoot.get("ids"), String[].class))
            {
               sfIds.add(s);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    public void savePersistentData()
    {
        String jsonData = "";
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode idRoot = mapper.createObjectNode();
        ArrayNode idArrayNode = mapper.createArrayNode();

        sfScan.log.logToOutput("[SFSCAN] Saving SFID tab data.");
        // Save targets
        for(String target: sfIds.toArray(new String[0]))
        {
            idArrayNode.add(target);
        }

        idRoot.set("ids", idArrayNode);

        try {
            jsonData = mapper.writeValueAsString(idRoot);
        } catch (JsonProcessingException e) {
            sfScan.log.logToError("[SFSCAN ERROR] Error saving SFID tab data.");
            throw new RuntimeException(e);
        }

        sfScan.api.persistence().extensionData().setString("sfscan_ids", jsonData);
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {

        // Collect message body for SF POST messages
        if(requestToBeSent.isInScope() && requestToBeSent.toolSource().toolType() == ToolType.PROXY && requestToBeSent.method().equalsIgnoreCase("POST") && requestToBeSent.hasParameter("message", HttpParameterType.BODY) && requestToBeSent.hasParameter("aura.context", HttpParameterType.BODY))
        {
            String decoded = safeDecode(requestToBeSent.bodyToString());
            synchronized (sfIds) {
                checkSFID(decoded);
            }
        }
        return RequestToBeSentAction.continueWith(requestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        // Collect message body for responses that are in scope and are less than 1 MB
        if(responseReceived.toolSource().toolType() == ToolType.PROXY && sfScan.api.scope().isInScope(responseReceived.initiatingRequest().url()) && responseReceived.body().length() < 1000000)
        {
            String decoded = safeDecode(responseReceived.bodyToString());
            synchronized (sfIds) {
                checkSFID(decoded);
            }
        }
        return ResponseReceivedAction.continueWith(responseReceived);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == saveButton) {
            saveData();
        }
        if (e.getSource() == copyTextItem)
        {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(textArea.getSelectedText()), null);
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
