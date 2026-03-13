package org.xfr.ui;

import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.params.HttpParameterType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.xfr.SFScan;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

public class SFIdTab extends JPanel implements ActionListener, HttpHandler {
    private final SFScan sfScan;
    private String currentContext;
    private JButton saveButton;
    private JTable idTable;
    private DefaultTableModel tableModel;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode treeRoot;
    private JTree tree;
    private HashMap<String, String[]> treeData;
    private TreeSet<String> sfIds;

    public SFIdTab(SFScan sfScan) {
        this.sfScan = sfScan;
        this.treeData = new HashMap<>();
        this.sfIds = new TreeSet<>();
        this.currentContext = "";
        buildPanelUI();
    }

    private void buildPanelUI() {
        this.setLayout(new BorderLayout());

        // Creating the root tree node
        treeRoot = new DefaultMutableTreeNode("SF IDs (0)");

        // Creating the JTree
        tree = new JTree(treeRoot);
        tree.setFont(new Font("Courier New", Font.PLAIN, 18));
        treeModel = (DefaultTreeModel) tree.getModel();

        saveButton = new JButton("Save Ids");
        JPanel buttonCollection = new JPanel();
        buttonCollection.setLayout(new GridLayout(1, 3));
        saveButton.setPreferredSize(new Dimension(100, 50));

        // Build panel
        idTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(idTable);

        // Add listeners
        saveButton.addActionListener(this);

        // Add items to panel
        buttonCollection.add(saveButton);
        this.add(new JScrollPane(tree));
        this.add(buttonCollection, BorderLayout.SOUTH);
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

    public static String safeDecode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return URLDecoder.decode(
                    s.replaceAll("%(?![0-9a-fA-F]{2})", "%25"),
                    StandardCharsets.UTF_8
            );
        }
    }

    private void addSFID(String id) {
        String prefix = id.substring(0, 3);

        // Check if ID is duplicate
        if(sfIds.contains(id))
        {
            return;
        }

        sfIds.add(id);

        // Check if ID prefix does not exist
        if(!treeData.containsKey(prefix))
        {
            ((DefaultMutableTreeNode)treeModel.getRoot()).add(new DefaultMutableTreeNode(prefix));
            treeData.put(prefix,new String[0]);
        }

        // Update tree
        Enumeration<TreeNode> children = ((DefaultMutableTreeNode)treeModel.getRoot()).children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode parent = ((DefaultMutableTreeNode)children.nextElement());
            // Check if parent prefix matches id prefix
            if(parent.getUserObject().toString().equals(prefix))
            {
                parent.add(new DefaultMutableTreeNode(id));
            }
        }
    }

    private String[] checkSFID(String content)
    {
        ArrayList<String> ids = new ArrayList<String>();
        // SF id pattern (yikes I know there is probably a better way)
        Pattern pattern = Pattern.compile("\\b[a-zA-Z0-9]{5}0[a-zA-Z0-9]{9}(?:[a-zA-Z0-9]{3})?\\b");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            addSFID(matcher.group().strip());
        }
        return ids.toArray(new String[0]);
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {

        // Collect message body for SF POST messages
        if(requestToBeSent.isInScope() && requestToBeSent.toolSource().toolType() == ToolType.PROXY && requestToBeSent.method().equalsIgnoreCase("POST") && requestToBeSent.hasParameter("message", HttpParameterType.BODY) && requestToBeSent.hasParameter("aura.context", HttpParameterType.BODY))
        {
            String decoded = safeDecode(requestToBeSent.bodyToString());
            checkSFID(decoded);
        }
        return RequestToBeSentAction.continueWith(requestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        // Collect message body for responses that are in scope and are less than 1 MB
        if(responseReceived.toolSource().toolType() == ToolType.PROXY && sfScan.api.scope().isInScope(responseReceived.initiatingRequest().url()) && responseReceived.body().length() < 1000000)
        {
            String decoded = safeDecode(responseReceived.bodyToString());
            checkSFID(decoded);
        }
        return ResponseReceivedAction.continueWith(responseReceived);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == saveButton) {
            saveData();
        }
    }
}
