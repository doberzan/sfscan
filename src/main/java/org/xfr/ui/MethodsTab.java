package org.xfr.ui;

import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.xfr.SFScan;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import static burp.api.montoya.http.handler.RequestToBeSentAction.continueWith;
import static burp.api.montoya.http.handler.ResponseReceivedAction.continueWith;

public class MethodsTab extends JPanel implements ActionListener, ListSelectionListener, HttpHandler, MouseListener {
    private final SFScan sfScan;
    private final ObjectMapper mapper;

    private JButton updateButton;
    private JButton outputButton;
    private JTextArea requestTextArea;
    private JLabel endpointsLabel;
    private JLabel parametersLabel;
    private JPopupMenu popupMenu;
    private JMenuItem copyTextItem;
    private JMenuItem sendToRepeaterItem;
    private JMenuItem sendToIntruderItem;

    public JList<String> methods;
    public JList<String> parameters;
    public DefaultListModel<String> methodsModel;
    public DefaultListModel<String> parametersModel;
    public HashMap<String, HttpRequest> exampleRequestForEndpoints;
    public HashMap<String, TreeSet<String>> endPoints;

    public MethodsTab(SFScan sfScan) {
        this.sfScan = sfScan;
        this.mapper = new ObjectMapper();
        this.setLayout(new BorderLayout());

        // Initialize objects
        endPoints = new HashMap<>();
        exampleRequestForEndpoints = new HashMap<>();
        methodsModel = new DefaultListModel<>();
        methods = new JList<>(methodsModel);
        parametersModel = new DefaultListModel<>();
        parameters = new JList<>(parametersModel);

        buildPanelUI();
    }

    private void buildPanelUI() {

        Font font = new Font("SansSerif", Font.BOLD, 16);


        // Build labels
        JLabel requestLabel = new JLabel(" Request");
        endpointsLabel = new JLabel(" API Endpoints");
        parametersLabel = new JLabel(" Parameters");
        endpointsLabel.setFont(font);
        parametersLabel.setFont(font);
        requestLabel.setFont(font);

        // Build request text area
        requestTextArea = new JTextArea();
        requestTextArea.setEditable(false);
        requestTextArea.setLineWrap(true);
        requestTextArea.setWrapStyleWord(true);
        addRightClickMenu(requestTextArea);

        // Build buttons
        updateButton = new JButton("Update");
        outputButton = new JButton("Save API Data");

        // Build scroll panes
        JScrollPane methodsScrollPane = new JScrollPane(methods);
        JScrollPane parametersScrollPane = new JScrollPane(parameters);
        JScrollPane requestAreaScrollPane = new JScrollPane(requestTextArea);

        // Build methods collection
        JPanel methodsCollection = new JPanel();
        methodsCollection.setLayout(new BorderLayout());
        methodsCollection.add(endpointsLabel, BorderLayout.NORTH);
        methodsCollection.add(methodsScrollPane, BorderLayout.CENTER);

        // Build parameters collection
        JPanel parametersCollection = new JPanel();
        parametersCollection.setLayout(new BorderLayout());
        parametersCollection.add(parametersLabel, BorderLayout.NORTH);
        parametersCollection.add(parametersScrollPane, BorderLayout.CENTER);

        // Build requests collection
        JPanel requestCollection = new JPanel();
        requestCollection.setLayout(new BorderLayout());
        requestCollection.add(requestLabel, BorderLayout.NORTH);
        requestCollection.add(requestAreaScrollPane, BorderLayout.CENTER);

        // Build button collection
        JPanel buttonCollection = new JPanel();
        buttonCollection.setLayout(new GridLayout(1, 2));
        buttonCollection.add(outputButton);
        buttonCollection.add(updateButton);

        // Build split pane containing parameters and methods
        JSplitPane leftPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        leftPanel.setRightComponent(parametersCollection);
        leftPanel.setLeftComponent(methodsCollection);

        // Build split pane containing requests panel and left-panel (parameters+methods)
        JSplitPane rightPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        rightPanel.setRightComponent(requestCollection);
        rightPanel.setLeftComponent(leftPanel);

        // Add listeners
        updateButton.addActionListener(this);
        outputButton.addActionListener(this);
        methods.addListSelectionListener(this);
        addRightClickMenu(methods);

        // Add items to main panel and set cosmetic preferences
        rightPanel.setBorder(null);
        leftPanel.setBorder(null);
        methodsCollection.setBorder(null);
        parametersCollection.setBorder(null);
        requestCollection.setBorder(null);
        requestTextArea.setBorder(null);
        updateButton.setPreferredSize(new Dimension(100, 50));
        methodsScrollPane.setBorder(null);
        parametersScrollPane.setBorder(null);
        requestAreaScrollPane.setBorder(null);
        this.add(rightPanel, BorderLayout.CENTER);
        this.add(buttonCollection, BorderLayout.SOUTH);
        leftPanel.setDividerLocation(450);
        rightPanel.setDividerLocation(1000);

    }

    private void addRightClickMenu(JComponent comp) {
        popupMenu = new JPopupMenu();
        sendToRepeaterItem = new JMenuItem("Send to Repeater");
        sendToIntruderItem = new JMenuItem("Send to Intruder");
        copyTextItem = new JMenuItem("Copy");

        sendToRepeaterItem.addActionListener(this);
        sendToIntruderItem.addActionListener(this);
        copyTextItem.addActionListener(this);

        popupMenu.add(copyTextItem);
        popupMenu.add(sendToRepeaterItem);
        popupMenu.add(sendToIntruderItem);

        comp.addMouseListener(this);

    }

    public void updateMethods(){
        methodsModel.clear();
        parametersModel.clear();

        // Sort list keys
        String[] tmp = endPoints.keySet().toArray(new String[0]);
        if (tmp.length > 0) {
            Arrays.sort(tmp);
        }

        // Add sorted elements to methods pane
        for (String s : tmp)
        {
            methodsModel.addElement(s +"\n");
        }
        endpointsLabel.setText(" API Endpoints ("+tmp.length+")");
    }

    private void updateParameters(){
        parametersModel.clear();
        if(methods.getSelectedValue() != null) {
            for (String param : endPoints.get(methods.getSelectedValue().strip())) {
                if(param != null) {
                    parametersModel.addElement(param);
                }
            }
            parametersLabel.setText(" Parameters ("+endPoints.get(methods.getSelectedValue().strip()).size()+")");
        }
    }

    private void printAPIList()
    {
        String[] tmp = endPoints.keySet().toArray(new String[0]);
        Arrays.sort(tmp);
        sfScan.log.logToOutput("======API Listing======\n");
        for (String s : tmp)
        {
            sfScan.log.logToOutput(s);
        }
    }

    private void updateRequestTextArea() {
        if(methods.getSelectedValue() != null) {
            requestTextArea.setText(exampleRequestForEndpoints.get(methods.getSelectedValue().strip()).toString());
        }
        requestTextArea.setCaretPosition(0);
    }

    private void saveData() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("api-list.txt"));

        if (fileChooser.showSaveDialog(this.getParent()) == JFileChooser.APPROVE_OPTION) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileChooser.getSelectedFile().getAbsolutePath()))) {
                String[] tmp = endPoints.keySet().toArray(new String[0]);
                Arrays.sort(tmp);
                for (String s : tmp)
                {
                    writer.write(s +"\n");
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private String parseSFAction(JsonNode action)
    {
        if(action.has("params") && action.get("params").has("classname") && action.get("params").has("method"))
        {
            return action.get("params").get("classname").textValue().strip() + "." + action.get("params").get("method").textValue().strip();
        }
        return null;
    }

    private void updateSFParameters(JsonNode action, TreeSet<String> params) {
        if(action.has("params") && action.get("params").has("classname") && action.get("params").has("params"))
        {
            Iterator<String> it = action.get("params").get("params").fieldNames();
            int failSafe = 100;
            while(it.hasNext() && failSafe > 0)
            {
                failSafe -= 1;
                String tmp = it.next();
                if(tmp != null) {
                    params.add(tmp);
                }
            }
        }
    }

    // Button pressed
    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == updateButton) {
            updateMethods();
        }
        if (e.getSource() == outputButton) {
            saveData();
        }
        if (e.getSource() == sendToRepeaterItem)
        {
            sfScan.api.repeater().sendToRepeater(exampleRequestForEndpoints.get(methods.getSelectedValue().strip()));
        }

        if (e.getSource() == sendToIntruderItem)
        {
            sfScan.api.intruder().sendToIntruder(exampleRequestForEndpoints.get(methods.getSelectedValue().strip()));
        }

        if (e.getSource() == copyTextItem)
        {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(requestTextArea.getSelectedText()), null);
        }

        printAPIList();
    }

    // New list item is selected on the methods tab
    @Override
    public void valueChanged(ListSelectionEvent e) {
        if(e.getSource() == methods)
        {
            updateRequestTextArea();
            updateParameters();
        }
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        // Collect message body for SF POST messages
        if(requestToBeSent.method().equalsIgnoreCase("POST") && requestToBeSent.hasParameter("message", HttpParameterType.BODY))
        {
            String message = URLDecoder.decode(requestToBeSent.parameterValue("message", HttpParameterType.BODY), StandardCharsets.UTF_8);
            try {
                JsonNode baseObject = mapper.readTree(message);
                for (JsonNode action : baseObject.get("actions")) {
                    String methodName = parseSFAction(action);
                    if(!endPoints.containsKey(methodName) && methodName != null) {
                        endPoints.put(methodName, new TreeSet<>());
                        updateMethods();
                        exampleRequestForEndpoints.put(methodName, requestToBeSent);
                    }
                    updateSFParameters(action, endPoints.get(methodName));
                }
            } catch (JsonProcessingException e) {
                sfScan.log.logToOutput("Error parsing JSON response: " + message);
            }

        }
        return continueWith(requestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        return continueWith(responseReceived);
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
