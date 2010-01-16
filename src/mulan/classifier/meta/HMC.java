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
 *    HMC.java
 *    Copyright (C) 2009 Aristotle University of Thessaloniki, Thessaloniki, Greece
 */
package mulan.classifier.meta;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import mulan.classifier.MultiLabelLearner;
import mulan.classifier.MultiLabelOutput;
import mulan.data.InvalidDataFormatException;
import mulan.data.LabelNode;
import mulan.data.LabelNodeImpl;
import mulan.data.LabelsMetaData;
import mulan.data.LabelsMetaDataImpl;
import mulan.data.MultiLabelInstances;
import mulan.transformations.RemoveAllLabels;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.TechnicalInformation;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Class that implements a Hierarchical Multilabel classifier (HMC).
 * HMC classifier takes as parameter any kind of multilabel classifier and
 * builds a hierarchy. Any node of hierarchy is a classifier and is trained
 * separately. The root classifier is trained on all data and as getting down
 * the hierarchy tree the data is adjusted properly to each node. Firstly,
 * instances that do not belong to the node are removed and then attributes that
 * are unnecessary are removed also.
 *
 * @author George Saridis
 * @author Grigorios Tsoumakas
 * @version 0.2
 */
public class HMC extends MultiLabelMetaLearner {

    private LabelsMetaData originalMetaData;    
    private HMCNode root;
    private Map<String, Integer> labelsAndIndices;
    private long NoNodes = 0;
    private long NoClassifierEvals = 0;
    private long TotalUsedTrainInsts = 0;

    public HMC(MultiLabelLearner baseLearner) throws Exception {
        super(baseLearner);
    }

    public HMC(MultiLabelLearner baseLearner, MultiLabelInstances dataSet) throws Exception {
        this(baseLearner);
        build(dataSet);
    }

    @Override
    public TechnicalInformation getTechnicalInformation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void buildRec(HMCNode node, Instances data) throws InvalidDataFormatException, Exception
    {
        String metaLabel = node.getName();
        debug(metaLabel);

        //debug("Preparing node data");
        Set<String> childrenLabels = new HashSet<String>();
        Set<String> currentlyAvailableLabels = new HashSet<String>();
        if (metaLabel.equals("root")) {
            for (LabelNode child : originalMetaData.getRootLabels())
                childrenLabels.add(child.getName());
            currentlyAvailableLabels = originalMetaData.getLabelNames();
        } else {
            LabelNode labelNode = originalMetaData.getLabelNode(metaLabel);
            for (LabelNode child : labelNode.getChildren())
                childrenLabels.add(child.getName());
            currentlyAvailableLabels = labelNode.getDescendantLabels();
        }

        // delete non-children labels
        Set<String> labelsToDelete = new HashSet(currentlyAvailableLabels);
        labelsToDelete.removeAll(childrenLabels);
        //System.out.println("Children: " + Arrays.toString(childrenLabels.toArray()));
        //System.out.println("Labels to delete:" + Arrays.toString(labelsToDelete.toArray()));

        int[] indicesToDelete = new int[labelsToDelete.size()];
        int counter1 = 0;
        for (String label : labelsToDelete) {
            indicesToDelete[counter1] = data.attribute(label).index();
            counter1++;
        }

        Remove filter1 = new Remove();
        filter1.setAttributeIndicesArray(indicesToDelete);
        filter1.setInputFormat(data);
        Instances nodeInstances = Filter.useFilter(data, filter1);
//        System.out.println()

        // create meta data
        LabelsMetaDataImpl nodeMetaData = new LabelsMetaDataImpl();
        for (String label : childrenLabels)
            nodeMetaData.addRootNode(new LabelNodeImpl(label));

        // create multi-label instance
        MultiLabelInstances nodeData = new MultiLabelInstances(nodeInstances, nodeMetaData);
        
        //debug("Building model");
        node.build(nodeData);
        //debug("spark #instances:"+nodeInstances.numInstances());
        TotalUsedTrainInsts += nodeInstances.numInstances();
        NoNodes++;
        //debug("spark:#nodes: "+ HMCNoNodes);
        
        for (String childLabel : childrenLabels) {
            LabelNode childNode = originalMetaData.getLabelNode(childLabel);
            if (!childNode.hasChildren())
                continue;
            //debug("Preparing child data");

            // remove instances where child is 0
            int childMetaLabelIndex = data.attribute(childLabel).index();
            Instances childData = new Instances(data);
            for (int i=0; i<childData.numInstances(); i++)
                if (childData.instance(i).stringValue(childMetaLabelIndex).equals("0")) {
                    childData.delete(i);
                    // While deleting an instance from the trainSet, i must reduced too
                    i--;
                }

            // delete non-descendant labels
            Set<String> descendantLabels = childNode.getDescendantLabels();
            Set<String> labelsToDelete2 = new HashSet(currentlyAvailableLabels);
            labelsToDelete2.removeAll(descendantLabels);
            //System.out.println("Labels to delete:" + Arrays.toString(labelsToDelete2.toArray()));
            int[] indicesToDelete2 = new int[labelsToDelete2.size()];
            int counter2 = 0;
            for (String label : labelsToDelete2) {
                indicesToDelete2[counter2] = childData.attribute(label).index();
                counter2++;
            }

            Remove filter2 = new Remove();
            filter2.setAttributeIndicesArray(indicesToDelete2);
            filter2.setInputFormat(childData);
            childData = Filter.useFilter(childData, filter2);

            MultiLabelLearner mll = baseLearner.makeCopy();
            HMCNode child = new HMCNode(childLabel, mll);
            node.addChild(child);

            buildRec(child, childData);            
        }      

    }

    @Override
    protected void buildInternal(MultiLabelInstances dataSet) throws Exception {
        originalMetaData = dataSet.getLabelsMetaData();
        Set<String> rootLabels = new HashSet<String>();
        for (LabelNode node : originalMetaData.getRootLabels())
            rootLabels.add(node.getName());



        MultiLabelLearner mll = baseLearner.makeCopy();
        root = new HMCNode("root", mll);
        buildRec(root, dataSet.getDataSet());
        labelsAndIndices = dataSet.getLabelsOrder();
    }

    public MultiLabelOutput makePrediction(Instance instance) throws Exception {
        boolean[] predictedLabels = new boolean[numLabels];
        double[] confidences = new double[numLabels];

        makePrediction(root, instance, predictedLabels, confidences);

        return new MultiLabelOutput(predictedLabels, confidences);
    }

    private void makePrediction(HMCNode currentNode, Instance instance, boolean[] predictedLabels, double[] confidences) throws Exception {
        String metaLabel = currentNode.getName();
        //System.out.println("Node: " + metaLabel);

        double[] values = instance.toDoubleArray();

        Instance transformed = new Instance(1, values);
        // delete all attributes (other way of transformation)
        transformed = RemoveAllLabels.transformInstance(transformed, labelIndices);
        transformed.setDataset(null);

        int numChildren;
        if (metaLabel.equals("root"))
            numChildren = originalMetaData.getRootLabels().size();
        else
            numChildren = originalMetaData.getLabelNode(metaLabel).getChildren().size();
        for (int j=0; j<numChildren; j++)
            transformed.insertAttributeAt(transformed.numAttributes());
        transformed.setDataset(currentNode.getHeader());
        // add as many attributes as the children    
//        System.out.println("header:" + currentNode.getHeader());
        //System.out.println(transformed.toString());

        //debug("working at node " + currentNode.getName());
        //debug(Arrays.toString(predictedLabels));        
        NoClassifierEvals++;        
        MultiLabelOutput pred = currentNode.makePrediction(transformed);
        int[] indices = currentNode.getLabelIndices();
        boolean[] temp = pred.getBipartition();

        for (int i=0; i<temp.length; i++)
        {
            String childName = currentNode.getHeader().attribute(indices[i]).name();
            //System.out.println("childName:" + childName);
            int idx = labelsAndIndices.get(childName);
            if (pred.getBipartition()[i] == true) {
                predictedLabels[idx] = true;
                confidences[idx] = pred.getConfidences()[i];
                if (currentNode.hasChildren())
                    for (HMCNode child : currentNode.getChildren())
                        if (child.getName().equals(childName))
                            makePrediction(child, instance, predictedLabels, confidences);
            } else {
                predictedLabels[idx] = false;
                Set<String> descendantLabels = originalMetaData.getLabelNode(childName).getDescendantLabels();
                if (descendantLabels != null)
                    for (String label : descendantLabels) {
                        int idx2 = labelsAndIndices.get(label);
                        predictedLabels[idx2] = false;
                        confidences[idx2] = pred.getConfidences()[i];
                    }
            }
        }
    }

    /**
     * Learns the Labels hierarchy and builds the appropriate tree for the
     * hierarchical multilabel classifier. The method is recursive.
     *
     * @param labels the set of labels that will be added to the HMC_node
     * @param currentNode the node to which the labels are added
     */
    private void learnHierarchy(Set<LabelNode> labels, HMCNode currentNode) throws Exception {
        for (LabelNode labelNode : labels) {
            HMCNode newHMCNode = new HMCNode(labelNode.getName(), baseLearner);
            if (labelNode.hasChildren()) {
                learnHierarchy(labelNode.getChildren(), newHMCNode);
            }
            currentNode.addChild(newHMCNode);
        }
    }



    /**
     * Deletes the unnecessary attributes. Actually keeps only the children
     * names of the node that is going to be trained as attributes and deletes
     * the rest.
     *
     * @param data the instances from which the attributes will be removed
     * @param labelName the name of the node whose children will be kept as attributes
     * @return MultiLabelInstances
     * @throws mulan.core.data.InvalidDataFormatException
     */
    protected MultiLabelInstances deleteLabels(MultiLabelInstances mlData, String currentLabel, boolean keepSubTree) throws InvalidDataFormatException {
        LabelsMetaData currentMetaData = mlData.getLabelsMetaData();
        LabelNodeImpl currentLabelNode = (LabelNodeImpl) currentMetaData.getLabelNode(currentLabel);

        Set<String> labelsToKeep;
        Set<String> allLabels = mlData.getLabelsMetaData().getLabelNames();
        LabelsMetaDataImpl labelsMetaData = new LabelsMetaDataImpl();

        //Prepare the appropriate labelsMetaData
        if(keepSubTree){
            labelsToKeep = currentLabelNode.getDescendantLabels();
            for (String rootLabel : currentLabelNode.getChildrenLabels()) {
                LabelNodeImpl rootNode = new LabelNodeImpl(rootLabel);
                if(mlData.getLabelsMetaData().getLabelNode(rootLabel).hasChildren()){
                    append(rootNode,mlData.getLabelsMetaData());
                }
                labelsMetaData.addRootNode(rootNode);
            }
        }else{
            labelsToKeep = currentLabelNode.getChildrenLabels();
            for(String rootLabel : labelsToKeep){
                LabelNodeImpl rootNode = new LabelNodeImpl(rootLabel);
                labelsMetaData.addRootNode(rootNode);
            }
        }
        
        debug("Labels: " + labelsMetaData.getLabelNames().toString());
        
        //Deleting labels from instances
        for (String label : allLabels) {
            if (!labelsToKeep.contains(label)) {
                int idx = mlData.getDataSet().attribute(label).index();
                mlData.getDataSet().deleteAttributeAt(idx);
            }
        }

        return new MultiLabelInstances(mlData.getDataSet(),labelsMetaData);
    }


    private void append(LabelNodeImpl labelNode,LabelsMetaData labelsMetaData){
        LabelNode father = labelsMetaData.getLabelNode(labelNode.getName());
        for(LabelNode child : father.getChildren()){
            LabelNodeImpl newLabelNode = new LabelNodeImpl(child.getName());
            if (child.hasChildren())
                append(newLabelNode,labelsMetaData);
            labelNode.addChildNode(newLabelNode);
        }
    }


    /**
     * Deletes the unnecessary instances, the instances that have value 0 on
     * given attribute.
     *
     * @param trainSet the trainSet on which the deletion will be applied
     * @param attrIndex the index of the attribute that the deletion is based
     */
    protected void deleteInstances(Instances trainSet, int attrIndex) {
        for (int i = 0; i < trainSet.numInstances(); i++) {
            if (trainSet.instance(i).stringValue(attrIndex).equals("0")) {
                trainSet.delete(i);
                // While deleting an instance from the trainSet, i must reduced too
                i--;
            }
        }
    }
    //spark temporary edit
    public long getNoNodes() {
      return NoNodes;
    }
    public long getNoClassifierEvals() {
    	return NoClassifierEvals;
    }
    public long getTotalUsedTrainInsts() {
    	return TotalUsedTrainInsts;    
    }
}