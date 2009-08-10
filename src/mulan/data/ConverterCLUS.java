/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    ConverterCLUS.java
 *    Copyright (C) 2009 Aristotle University of Thessaloniki, Thessaloniki, Greece
 */
package mulan.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

/**
 * Class that converts the datasets in CLUS format to Mulan compatible dataset.
 * An arff and xml file are created. The arff file contains the original
 * dataset with all labels converted to separate attributes and properly
 * converted instances. The xml file contains the hierarchy of the labels.
 *
 * @author George Saridis
 */
public class ConverterCLUS {

    private static String[] hierarchicalAtts = null;
    private static ArrayList<String> labelVals = null;

    /**
     * Converts the original dataset to mulan compatible dataset.The output
     * files will be named "converted " + source's file name.
     *
     * @param sourceFileName the sourceFileName
     * @throws java.lang.Exception
     */
    public static void convert(String sourceFileName) throws Exception {
        String arffName = "converted " + sourceFileName;
        String xmlName = "converted " + sourceFileName.split(".arff", 2)[0] + ".xml";
        convert(sourceFileName, arffName, xmlName);
    }

    /**
     * Converts the original dataset to mulan compatible dataset.
     *
     * @param sourceFileName the source file name
     * @param arffName the converted arff name
     * @param xmlName the xml name
     * @throws java.lang.Exception
     */
    public static void convert(String sourceFileName, String arffName, String xmlName) throws Exception {
        Instances dataSet = readDataSet(sourceFileName,arffName);
        Instances convertedDataSet = convertInstances(dataSet);
        saveDataSet(arffName, convertedDataSet);
        LabelsMetaData labelsMetaData = createLabelsMetaData();
        Document labelsXMLDoc = createXMLFile(labelsMetaData);
        saveToXMLFile(xmlName, labelsXMLDoc);
    }

    private static Instances readDataSet(String sourceFileName,String arffName) {
        String line;
        Instances dataSet = null;
        try {
            File sourceFile = new File(sourceFileName);
            FileReader frInput = new FileReader(sourceFile);
            BufferedReader brInput = new BufferedReader(frInput);
            
            BufferedWriter convertedFile = new BufferedWriter(new FileWriter(arffName));
            while (!(line = brInput.readLine()).contains("class")) {
                convertedFile.write(line + "\n");
            }

            line = line.split("hierarchical ", 2)[1];
            hierarchicalAtts = line.split(",");

            brInput.readLine(); // skip the blank line
            convertedFile.write(brInput.readLine() + "\n");
            String[] data;
            labelVals = new ArrayList();

            while ((line = brInput.readLine()) != null) {
                data = line.split(",");
                for (int i = 0; i < data.length - 1; i++) {
                    convertedFile.write(data[i]);
                    if (i < (data.length - 2)) {
                        convertedFile.write(",");
                    }
                }
                convertedFile.write("\n");
                labelVals.add(data[data.length - 1]);

            }
            convertedFile.close();
            dataSet = new Instances(new FileReader(arffName));
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        }
        return dataSet;
    }

    private static Instances convertInstances(Instances dataSet) {
        FastVector atts = new FastVector();
        for (int i = 0; i < dataSet.numAttributes(); i++) {
            atts.addElement(dataSet.attribute(i));
        }

        FastVector labelValues = new FastVector();
        labelValues.addElement("0");
        labelValues.addElement("1");

        for (int i = 0; i < hierarchicalAtts.length; i++) {
            atts.addElement(new Attribute(hierarchicalAtts[i], labelValues));
        }

        Instances convertedDataSet = new Instances(dataSet.relationName(), atts, dataSet.numInstances());

        for (int i = 0; i < dataSet.numInstances(); i++) {
            double[] newValues = new double[convertedDataSet.numAttributes()];
            Arrays.fill(newValues, 0);

            double[] values = dataSet.instance(i).toDoubleArray();
            System.arraycopy(values, 0, newValues, 0, values.length);

            String[] labels = labelVals.get(i).split("@");

            for (int j = 0; j < labels.length; j++) {
                String[] splitedLabels = labels[j].split("/");
                String attr = splitedLabels[0];
                Attribute at = convertedDataSet.attribute(attr);
                newValues[atts.indexOf(at)] = 1;
                for (int k = 1; k < splitedLabels.length; k++) {
                    attr = attr + "/" + splitedLabels[k];
                    at = convertedDataSet.attribute(attr);
                    newValues[atts.indexOf(at)] = 1;
                }
            }
            convertedDataSet.add(new Instance(dataSet.instance(i).weight(), newValues));
        }

        return convertedDataSet;
    }

    private static LabelsMetaDataImpl createLabelsMetaData() {
        LabelsMetaDataImpl labelsMetaData = new LabelsMetaDataImpl();
        ArrayList<LabelNodeImpl> rootLabels = new ArrayList();
        Map<String, LabelNodeImpl> allLabelNodes = new HashMap<String, LabelNodeImpl>();
        for (int i = 0; i < hierarchicalAtts.length; i++) {
            String hierarchicalLabels = hierarchicalAtts[i];
            int idx = hierarchicalLabels.lastIndexOf("/");
            if (idx != -1) {
                String newLabel = hierarchicalLabels.substring(idx + 1);
                String parentLabel = hierarchicalLabels.substring(0, idx);

                LabelNodeImpl label = new LabelNodeImpl(parentLabel + "/" + newLabel);
                allLabelNodes.put(parentLabel + "/" + newLabel, label);

                LabelNodeImpl parent = allLabelNodes.get(parentLabel);
                parent.addChildNode(label);
            } else {
                LabelNodeImpl rootNode = new LabelNodeImpl(hierarchicalLabels);
                rootLabels.add(rootNode);
                allLabelNodes.put(hierarchicalLabels, rootNode);
            }
        }
        for (int i = 0; i < rootLabels.size(); i++) {
            labelsMetaData.addRootNode(rootLabels.get(i));
        }
        return labelsMetaData;
    }

    private static Document createXMLFile(LabelsMetaData metaData) throws Exception {
        DocumentBuilderFactory docBF = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBF.newDocumentBuilder();
        Document labelsXMLDoc = docBuilder.newDocument();

        Element rootElement = labelsXMLDoc.createElement("labels");
        rootElement.setAttribute("xmlns", "http://mulan.sourceforge.net/labels");
        labelsXMLDoc.appendChild(rootElement);
        for (LabelNode rootLabel : metaData.getRootLabels()) {
            Element newLabelElem = labelsXMLDoc.createElement("label");
            newLabelElem.setAttribute("name", rootLabel.getName());
            appendElement(newLabelElem, rootLabel, labelsXMLDoc);
            rootElement.appendChild(newLabelElem);
        }
        return labelsXMLDoc;
    }

    private static void appendElement(Element labelElem, LabelNode labelNode, Document labelsXMLDoc) {
        for (LabelNode childNode : labelNode.getChildren()) {
            Element newLabelElem = labelsXMLDoc.createElement("label");
            newLabelElem.setAttribute("name", childNode.getName());
            appendElement(newLabelElem, childNode, labelsXMLDoc);
            labelElem.appendChild(newLabelElem);
        }
    }

    private static void saveToXMLFile(String xmlName, Document labelsXMLDoc) {
        Source source = new DOMSource(labelsXMLDoc);
        File xmlFile = new File(xmlName);
        StreamResult result = new StreamResult(xmlFile);
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.transform(source, result);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void saveDataSet(String arffName, Instances dataSet) throws IOException {
        ArffSaver saver = new ArffSaver();
        saver.setInstances(dataSet);
        saver.setFile(new File(arffName));
        saver.writeBatch();
    }
}