package it.unisa.ascetic.gui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.table.JBTable;
import it.unisa.ascetic.analysis.code_smell.CodeSmell;
import it.unisa.ascetic.storage.beans.ClassBean;
import it.unisa.ascetic.storage.beans.MethodBean;
import it.unisa.ascetic.storage.beans.PackageBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import src.main.java.it.unisa.ascetic.gui.StyleText;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class CheckProjectPage extends DialogWrapper {

    private DefaultTableModel model;

    private Project currentProject;
    private List<PackageBean> promiscuousPackageList;
    private List<MethodBean> featureEnvyList;
    private List<ClassBean> misplacedClassList;
    private List<ClassBean> blobList;
    private JPanel pannello;
    private JTextPane codeVisual;
    private JTable table;
    private DecimalFormat df = new DecimalFormat("0.000");
    private DecimalFormat df2 = new DecimalFormat("0");
    private JPanel contentPanel;
    private JPanel smellVisual;
    private JPanel panel;
    private JPanel textual;
    private JPanel structural;
    private JTextField valCoseno;
    private ArrayList<JTextField> valDipendenza;
    private JLabel soglia2;

    private JPanel centerPanel;
    private JPanel nuovo;
    private JPanel slider;
    private JSlider dipendenze;
    private JSlider coseno;
    private JPanel smell;

    private ArrayList<JPanel> smellPanel;
    private JPanel featurePanel;
    private JPanel misPanel;
    private JPanel blobPanel;
    private JPanel promiscuousPanel;
    private HashMap<String, JCheckBox> codeSmell = new HashMap<String, JCheckBox>();
    private HashMap<String, JCheckBox> algoritmi = new HashMap<String, JCheckBox>();
    private HashMap<String, Integer> threshold = new HashMap<String, Integer>();
    private ArrayList<JPanel> algo;

    private static ArrayList<String> smellName;
    private static ArrayList<String> blobThresholdName;
    private int maxS = 0;
    private String algorithm;
    private double sogliaCoseno;
    private ArrayList<Integer> sogliaDipendenze;

    public CheckProjectPage(Project currentProj, List<PackageBean> promiscuous, List<ClassBean> blob, List<ClassBean> misplaced, List<MethodBean> feature, double sogliaCoseno, ArrayList<Integer> sogliaDipendenze, String algorithm) {
        super(true);
        smellName = new ArrayList<String>();
        smellName.add("Feature Envy");
        smellName.add("Misplaced Class");
        smellName.add("Blob");
        smellName.add("Promiscuous Package");
        blobThresholdName = new ArrayList<String>();
        blobThresholdName.add("LCOM");
        blobThresholdName.add("FeatureSUM");
        blobThresholdName.add("ELOC");

        this.currentProject = currentProj;
        this.promiscuousPackageList = promiscuous;
        this.featureEnvyList = feature;
        this.misplacedClassList = misplaced;
        this.blobList = blob;
        this.algorithm = algorithm;
        this.sogliaCoseno = sogliaCoseno;
        this.sogliaDipendenze = sogliaDipendenze;
        setResizable(false);
        init();
        setTitle("CODE SMELL ANALYSIS");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {

        threshold = new HashMap<String, Integer>();
        int i = 0;
        while (i < blobThresholdName.size()) {
            threshold.put(blobThresholdName.get(i), sogliaDipendenze.get(i + 1));
            i++;
        }

        contentPanel = new JPanel(); //pannello principale
        contentPanel.setLayout(new BorderLayout(0, 0));
        smellVisual = new JPanel(); //pannello per visualizzare lista di smell
        codeVisual = new JTextPane(); //pannello per visualizzare il codice dello smell

        smellVisual.setPreferredSize(new Dimension(350, 800));

        //roba della combobox
        panel = new JPanel();
        Dimension maxSize = new Dimension(Short.MAX_VALUE, 100);
        panel.setMaximumSize(maxSize);
        panel.setLayout(new BorderLayout(0, 0));
        //fine roba della combobox

        contentPanel.setLayout(new GridLayout(0, 2));//layout pannello principale
        contentPanel.setPreferredSize(new Dimension(1250, 900));
        smellVisual.setLayout(new BorderLayout(0, 0));
        smellVisual.add(panel, BorderLayout.NORTH);

        pannello = new JPanel();
        panel.add(pannello, BorderLayout.NORTH);

        centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        nuovo = new JPanel();
        centerPanel.add(nuovo);
        nuovo.setLayout(new BoxLayout(nuovo, BoxLayout.X_AXIS));
        smell = new JPanel();
        nuovo.add(smell);
        smell.setLayout(new BoxLayout(smell, BoxLayout.Y_AXIS));

        algo = new ArrayList<JPanel>();
        smellPanel = new ArrayList<JPanel>();
        for (i = 0; i < 4; i++) {
            smellPanel.add(new JPanel());
            smellPanel.get(i).setBorder(new EmptyBorder(0, 0, 10, 0));
            smellPanel.get(i).setLayout(new GridLayout(0, 2, 0, 0));
            smell.add(smellPanel.get(i));
            codeSmell.put(smellName.get(i), new JCheckBox(smellName.get(i)));
            smellPanel.get(i).add(codeSmell.get(smellName.get(i)));
            algo.add(new JPanel());
            smellPanel.get(i).add(algo.get(i));
            algo.get(i).setLayout(new GridLayout(0, 1, 0, 0));
            algoritmi.put("textual" + smellName.get(i).substring(0, 1), new JCheckBox("Textual"));
            algo.get(i).add(algoritmi.get("textual" + smellName.get(i).substring(0, 1)));
            algoritmi.put("structural" + smellName.get(i).substring(0, 1), new JCheckBox("Structural"));
            algo.get(i).add(algoritmi.get("structural" + smellName.get(i).substring(0, 1)));
            if (i == 2) algo.get(i).add(new JLabel("*non utilizza le dipendenze  "));
        }

        for (JCheckBox c : codeSmell.values()) {
            c.setSelected(true);
            c.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent a) {
                    JCheckBox cb = (JCheckBox) a.getSource();
                    createTable();
                    table.repaint();
                }
            });
        }

        JCheckBox c;
        for (String s : algoritmi.keySet()) {

            c = algoritmi.get(s);
            c.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent a) {
                    createTable();
                    table.repaint();
                }
            });
            if (algorithm.contains("All")) {
                c.setSelected(true);
            } else {
                if ((s.contains("textual") && algorithm.contains("Textual")) || (s.contains("structural") && algorithm.contains("Structural"))) {
                    c.setSelected(true);
                }
            }

            if (s.equalsIgnoreCase("structuralp")) {
                c.setEnabled(false);
                c.setSelected(false);
            }
        }

        slider = new JPanel();
        nuovo.add(slider);
        slider.setLayout(new BoxLayout(slider, BoxLayout.Y_AXIS));

        textual = new JPanel();
        textual.setBorder(new TitledBorder("Textual"));
        slider.add(textual);
        textual.setLayout(new BoxLayout(textual, BoxLayout.Y_AXIS));

        JPanel bar1 = new JPanel();
        textual.add(bar1);
        JPanel s = new JPanel();
        JLabel soglia1 = new JLabel();
        soglia1.setText("similarity >= [" + sogliaCoseno + "-1]");
        textual.add(s);
        s.add(soglia1);

        coseno = new JSlider();
        coseno.setForeground(Color.WHITE);
        coseno.setFont(new Font("Arial", Font.PLAIN, 12));
        coseno.setPaintTicks(true);
        coseno.setMinorTickSpacing(10);
        if (sogliaCoseno <= 0.5) {
            coseno.setMinimum((int) (sogliaCoseno * 100));
        } else {
            coseno.setMinimum(50);
        }
        ;
        coseno.setValue((int) (sogliaCoseno * 100));
        bar1.add(coseno);

        coseno.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                valCoseno.setText(String.valueOf(((double) coseno.getValue()) / 100));
                createTable();
                table.repaint();
            }
        });

        valCoseno = new JTextField();
        valCoseno.setText(String.valueOf(((double) coseno.getValue()) / 100));
        s.add(valCoseno);
        valCoseno.setColumns(5);

        valCoseno.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent c) {

                try {
                    JTextField t = (JTextField) c.getSource();
                    double valore = Double.parseDouble(t.getText());
                    if (valore >= 0.0 && valore <= 1.0) {
                        coseno.setValue((int) (valore * 100));
                    } else {
                        if (valore < 0.0) {
                            coseno.setValue(0);
                            valCoseno.setText(0.0 + "");
                        } else {
                            if (valore > 1.0) {
                                coseno.setValue(100);
                                valCoseno.setText(1.0 + "");
                            } else {
                                coseno.setValue((int) valore);
                                valCoseno.setText(valore + "");
                            }
                        }
                        ;
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(null, "Inserire valori decimali con \".\" [" + sogliaCoseno + "-1]", "Errore", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        structural = new JPanel();
        structural.setBorder(new TitledBorder("Structural"));
        slider.add(structural);
        structural.setLayout(new BoxLayout(structural, BoxLayout.Y_AXIS));

        JPanel bar2 = new JPanel();
        structural.add(bar2);
        JPanel s2 = new JPanel();
        soglia2 = new JLabel();
        structural.add(s2);
        s2.add(soglia2);

        dipendenze = new JSlider();
        dipendenze.setForeground(Color.WHITE);
        dipendenze.setFont(new Font("Arial", Font.PLAIN, 12));
        dipendenze.setValue(sogliaDipendenze.get(0));
        dipendenze.setPaintTicks(true);
        dipendenze.setMinorTickSpacing(1);
        dipendenze.setMinimum(0);
        bar2.add(dipendenze);

        dipendenze.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                try {
                    valDipendenza.get(0).setText(String.valueOf(dipendenze.getValue()));
                    createTable();
                    table.repaint();
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(null, "Inserire valori interi", "", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        valDipendenza = new ArrayList<JTextField>();
        valDipendenza.add(new JTextField());
        s2.add(valDipendenza.get(0));
        valDipendenza.get(0).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent c) {
                JTextField t = (JTextField) c.getSource();
                dipendenze.setValue(Integer.parseInt(t.getText()));
            }
        });

        JPanel livello, blobThreshold = new JPanel();
        blobThreshold.setBorder(new TitledBorder("Blob"));
        structural.add(blobThreshold);
        blobThreshold.setLayout(new BoxLayout(blobThreshold, BoxLayout.Y_AXIS));
        JLabel scritta;
        for (i = 0; i < 4; i++) {
            if (i != 0) {
                valDipendenza.add(new JTextField());
                valDipendenza.get(i).setText(df2.format(threshold.get(blobThresholdName.get(i - 1))));

                livello = new JPanel();
                livello.setLayout(new GridLayout(2, 2, 0, 0));
                scritta = new JLabel(blobThresholdName.get(i - 1));
                scritta.setHorizontalAlignment(SwingConstants.CENTER);
                livello.add(scritta);
                livello.add(valDipendenza.get(i));
                scritta = new JLabel("valore min=" + df2.format(threshold.get(blobThresholdName.get(i - 1))));
                scritta.setHorizontalAlignment(SwingConstants.RIGHT);
                livello.add(scritta);
                blobThreshold.add(livello);
            }
            valDipendenza.get(i).setText(String.valueOf(sogliaDipendenze.get(i)));
            valDipendenza.get(i).setColumns(5);
        }

        valDipendenza.get(1).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent c) {
                JTextField f = (JTextField) c.getSource();
                if (Integer.parseInt(f.getText()) < threshold.get(blobThresholdName.get(0))) {
                    f.setText("350");
                    JOptionPane.showMessageDialog(null, "Soglia minima consentita " + df2.format(threshold.get(blobThresholdName.get(0))));
                }
                createTable();
                table.repaint();
            }
        });
        valDipendenza.get(2).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent c) {
                JTextField f = (JTextField) c.getSource();
                if (Integer.parseInt(f.getText()) < threshold.get(blobThresholdName.get(1))) {
                    f.setText("20");
                    JOptionPane.showMessageDialog(null, "Soglia minima consentita " + df2.format(threshold.get(blobThresholdName.get(1))));
                }
                createTable();
                table.repaint();
            }
        });
        valDipendenza.get(3).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent c) {
                JTextField f = (JTextField) c.getSource();
                if (Integer.parseInt(f.getText()) < threshold.get(blobThresholdName.get(2))) {
                    f.setText("500");
                    JOptionPane.showMessageDialog(null, "Soglia minima consentita " + df2.format(threshold.get(blobThresholdName.get(2))));
                }
                createTable();
                table.repaint();
            }
        });

        pannello.add(centerPanel);
        createTable();

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() != false) {
                    setArea();
                }
            }
        });

        JPanel text = new JPanel();
        text.setLayout(new BorderLayout(0, 0));
        text.setBorder(new TitledBorder("Text content"));
        JPanel tab = new JPanel();
        tab.setLayout(new BorderLayout(0, 0));
        tab.setBorder(new TitledBorder("List"));
        contentPanel.setLayout(new GridLayout(0, 2));//layout pannello principale

        JScrollPane scroll = new JScrollPane(table);
        tab.add(scroll, BorderLayout.CENTER);// aggiunta tabella smell
        smellVisual.add(tab);

        contentPanel.add(smellVisual);

        codeVisual.setEditable(false);
        JScrollPane scroll2 = new JScrollPane(codeVisual);
        text.add(scroll2, BorderLayout.CENTER);
        contentPanel.add(text);

        return contentPanel;
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        Action okAction = new DialogWrapperAction("INSPECT") {

            @Override
            protected void doAction(ActionEvent actionEvent) {
                try {
                    String whatToReturn; //fullqualified name dell'elemento selezionato nella tabella
                    String whereToSearch; //tipo di smell, indice della lista dove cercare il bean
                    whatToReturn = (String) table.getValueAt(table.getSelectedRow(), 0);
                    whereToSearch = (String) table.getValueAt(table.getSelectedRow(), 1);
                    if (whereToSearch.equalsIgnoreCase("blob")) {
                        for (ClassBean c : blobList) {
                            if (c.getFullQualifiedName().equalsIgnoreCase(whatToReturn)) {
                                DialogWrapper blob = new BlobPage(c, currentProject);
                                blob.show();
                            }
                        }
                    }

                    if (whereToSearch.equalsIgnoreCase("feature envy")) {
                        for (MethodBean m : featureEnvyList) {
                            if (m.getFullQualifiedName().equalsIgnoreCase(whatToReturn)) {
                                DialogWrapper feat = new FeatureEnvyPage(m, currentProject);
                                feat.show();
                            }
                        }
                    }

                    if (whereToSearch.equalsIgnoreCase("promiscuous package")) {
                        for (PackageBean p : promiscuousPackageList) {
                            if (p.getFullQualifiedName().equalsIgnoreCase(whatToReturn)) {
                                DialogWrapper prom = new PromiscuousPackagePage(p, currentProject);
                                prom.show();
                            }
                        }
                    }

                    if (whereToSearch.equalsIgnoreCase("misplaced class")) {
                        for (ClassBean c : misplacedClassList) {
                            if (c.getFullQualifiedName().equalsIgnoreCase(whatToReturn)) {
                                DialogWrapper mis = new MisplacedClassPage(c, currentProject);
                                mis.show();
                            }
                        }
                    }

                } catch (ArrayIndexOutOfBoundsException ex) {
                    String message = "Seleziona un elemento";
                    Messages.showMessageDialog(message, "Warning", Messages.getWarningIcon());
                }
            }
        };

        return new Action[]{okAction, new DialogWrapperExitAction("EXIT", 0)};
    }

    private void createTable() {

        Vector<String> columnNames = new Vector<>();
        columnNames.add("Member name");
        columnNames.add("Smell detected");
        columnNames.add("Textual algorithm");
        columnNames.add("Structural algorithm");
        columnNames.add("priority");
        model = new DefaultTableModel(columnNames, 0);

        if (blobList != null) {
            if (codeSmell.get("Blob").isSelected()) {
                for (ClassBean c : blobList) {
                    gestione(c.getAffectedSmell(), "Blob", c.getFullQualifiedName());
                }
            }
        }

        if (misplacedClassList != null) {
            if (codeSmell.get("Misplaced Class").isSelected()) {
                for (ClassBean c : misplacedClassList) {
                    gestione(c.getAffectedSmell(), "Misplaced Class", c.getFullQualifiedName());
                }
            }
        }

        if (promiscuousPackageList != null) {
            if (codeSmell.get("Promiscuous Package").isSelected()) {
                for (PackageBean pp : promiscuousPackageList) {
                    gestione(pp.getAffectedSmell(), "Promiscuous Package", pp.getFullQualifiedName());
                }
            }
        }

        if (featureEnvyList != null) {
            if (codeSmell.get("Feature Envy").isSelected()) {
                for (MethodBean m : featureEnvyList) {
                    gestione(m.getAffectedSmell(), "Feature Envy", m.getFullQualifiedName());
                }
            }
        }

        if (this.table == null) {
            JTable table = new JBTable();
            this.table = table;
        }

        soglia2.setText("dipendenze >= [" + "0" + "-" + maxS + "]");
        dipendenze.setMaximum(maxS);
        this.table.setModel(model);
        table.setDefaultEditor(Object.class, null);

    }

    private void gestione(List<CodeSmell> list, String codeSmell, String bean) {

        boolean basso = false;
        int alto = 0;
        int complessita = 0;
        Vector<String> tableItem;
        String used;

        double cos = 0.0, indice = 0.0;
        int dip = 0;
        boolean controllo = false;
        int i = 0;

        for (CodeSmell smell : list) {

            if (smell.getSmellName().equalsIgnoreCase(codeSmell)) {
                used = smell.getAlgoritmsUsed();
                tableItem = new Vector<String>();

                HashMap<String, Double> listThreschold = smell.getIndex();
                if (algoritmi.get("textual" + codeSmell.substring(0, 1)).isSelected() && listThreschold.get("coseno") != null) {
                    if (used.equalsIgnoreCase("textual") && Double.parseDouble(valCoseno.getText()) <= listThreschold.get("coseno")) {

                        complessita++;
                        indice = Double.parseDouble(valCoseno.getText());
                        cos = listThreschold.get("coseno");
                        if (cos <= sogliaCoseno) {
                            basso = true;
                        }
                        ;
                        if (cos >= 0.75) {
                            alto++;
                        }
                        ;
                    } else {
                        controllo = true;
                    }
                } else {
                    controllo = true;
                }

                if (algoritmi.get("structural" + codeSmell.substring(0, 1)).isSelected() && listThreschold.get("dipendenza") != null) {
                    if (used.equalsIgnoreCase("structural") && Double.parseDouble(valDipendenza.get(0).getText()) <= listThreschold.get("dipendenza")) {

                        complessita += 2;
                        indice = Double.parseDouble(valDipendenza.get(0).getText());
                        dip = listThreschold.get("dipendenza").intValue();

                        if (dip <= sogliaDipendenze.get(0)) {
                            basso = true;
                        }
                        ;
                        if (dip >= (int) (maxS - (maxS * 0.25))) {
                            alto++;
                        }
                        ;
                        if (dip >= maxS) {
                            maxS = dip;
                        }
                        ;
                    }
                } else {
                    if (smell.getSmellName().equalsIgnoreCase("blob") && algoritmi.get("structural" + codeSmell.substring(0, 1)).isSelected() && used.equalsIgnoreCase("structural")) {
                        Double lcom = Double.parseDouble(valDipendenza.get(1).getText());
                        Double fsum = Double.parseDouble(valDipendenza.get(2).getText());
                        Double eloc = Double.parseDouble(valDipendenza.get(3).getText());
                        if (lcom <= listThreschold.get("LCOM") && fsum <= listThreschold.get("featureSum") && eloc <= listThreschold.get("ELOC")) {
                            complessita += 2;
                            alto++;
                            if (lcom >= listThreschold.get("LCOM") || fsum >= listThreschold.get("featureSum") || eloc >= listThreschold.get("ELOC")) {
                                basso = true;
                            }
                        }
                    } else {
                        controllo = true;
                    }
                }


                if (i + 1 >= list.size() || !list.get(i + 1).getSmellName().equalsIgnoreCase(smell.getSmellName())) {

                    if (((cos != 0.0 || dip != 0) && ((listThreschold.get("coseno") != null && indice <= listThreschold.get("coseno") && algoritmi.get("textual" + codeSmell.substring(0, 1)).isSelected()) ||
                            (listThreschold.get("dipendenza") != null && indice <= listThreschold.get("dipendenza") && algoritmi.get("structural" + codeSmell.substring(0, 1)).isSelected()))) ||
                            (smell.getSmellName().equalsIgnoreCase("Blob") && smell.getAlgoritmsUsed().equalsIgnoreCase("structural") && algoritmi.get("structural" + codeSmell.substring(0, 1)).isSelected() &&
                                    Double.parseDouble(valDipendenza.get(1).getText()) <= listThreschold.get("LCOM") && Double.parseDouble(valDipendenza.get(2).getText()) <= listThreschold.get("featureSum") && Double.parseDouble(valDipendenza.get(3).getText()) <= listThreschold.get("ELOC"))) {
                        tableItem.add(bean);
                        tableItem.add(smell.getSmellName());
                        if (cos == 0.0) {
                            tableItem.add("---");
                        } else {
                            tableItem.add(df.format(cos));
                        }
                        ;
                        if (dip == 0) {
                            if (smell.getSmellName().equalsIgnoreCase("Blob") && algoritmi.get("structural" + codeSmell.substring(0, 1)).isSelected() && alto > 0) {
                                HashMap<String, Double> soglie = smell.getIndex();
                                tableItem.add(df2.format(soglie.get("LCOM")) + "-" + df2.format(soglie.get("featureSum")) + "-" + df2.format(soglie.get("ELOC")));
                            } else {
                                tableItem.add("---");
                            }
                            ;
                        } else {
                            tableItem.add(df2.format(dip));
                        }
                        ;
                        tableItem.add(prioritySmell(controllo, complessita, basso, alto));
                        model.addRow(tableItem);
                        cos = 0.0;
                        dip = 0;
                        complessita = 0;
                        alto = 0;
                        basso = false;
                        controllo = false;
                    }
                    indice = 0.0;
                }
            }
            i++;
        }

    }

    private String prioritySmell(boolean controllo, int complessita, boolean basso, int alto) {

        if (complessita <= 2 && !controllo && alto < 1) {
            return "bassa";
        } else {
            if (!basso) {
                switch (alto) {
                    case 1:
                        return "alto";
                    case 2:
                        return "urgente";
                    default:
                        return "media";
                }
            }
            return "media";
        }
    }

    private void setArea() {
        String whatToReturn; //fullqualified name dell'elemento selezionato nella tabella
        String whereToSearch; //tipo di smell, indice della lista dove cercare il bean
        try {
            whatToReturn = (String) table.getValueAt(table.getSelectedRow(), 0);
            whereToSearch = (String) table.getValueAt(table.getSelectedRow(), 1);
            String textContent = null;

            if (whereToSearch.equalsIgnoreCase("blob")) {
                for (ClassBean c : blobList) {
                    if (c.getFullQualifiedName().equalsIgnoreCase(whatToReturn)) {
                        textContent = c.getTextContent();
                    }
                }
            } else {
                if (whereToSearch.equalsIgnoreCase("feature envy")) {
                    for (MethodBean m : featureEnvyList) {
                        if (m.getFullQualifiedName().equalsIgnoreCase(whatToReturn)) {
                            textContent = m.getTextContent();
                        }
                    }
                } else {
                    if (whereToSearch.equalsIgnoreCase("promiscuous package")) {
                        for (PackageBean p : promiscuousPackageList) {
                            if (p.getFullQualifiedName().equalsIgnoreCase(whatToReturn)) {
                                textContent = p.getTextContent();
                            }
                        }
                    } else {
                        if (whereToSearch.equalsIgnoreCase("misplaced class")) {
                            for (ClassBean c : misplacedClassList) {
                                if (c.getFullQualifiedName().equalsIgnoreCase(whatToReturn)) {
                                    textContent = c.getTextContent();
                                }
                            }
                        }
                    }
                }
            }

            StyleText generator = new StyleText();
            codeVisual.setStyledDocument(generator.createDocument(textContent));
        } catch (ArrayIndexOutOfBoundsException ex) {
        }
    }
}