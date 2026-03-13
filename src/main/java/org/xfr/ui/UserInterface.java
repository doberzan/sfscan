package org.xfr.ui;

import org.xfr.SFScan;

import javax.swing.*;

public class UserInterface extends JTabbedPane {
    public MethodsTab methodsTab;
    public ObjectsTab objectsTab;
    public RecordsTab recordsTab;
    public SFIdTab sfIdTab;
    public TargetTab targetTab;

    public UserInterface(SFScan sfScan) {
        this.methodsTab = new MethodsTab(sfScan);
        this.objectsTab = new ObjectsTab(sfScan);
        this.sfIdTab = new SFIdTab(sfScan);
        this.targetTab = new TargetTab(sfScan);

        this.add("Targets",targetTab);
        this.add("Methods",methodsTab);
        this.add("Objects",objectsTab);
        this.add("Ids",sfIdTab);
    }

}