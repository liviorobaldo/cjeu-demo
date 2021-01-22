package SDFTagger;

import java.io.*;
import java.util.*;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.*;
import SDFTagger.SDFItems.*;

public class SDFLogger 
{
    /**
    
    The SDFDebug file is an XML structured in four parts:

    <SDFDebugs>
        <SDFDebug>
            <SDFNodes>...</SDFNodes>
            <SDFRules>...</SDFRules>
            <SDFTraces>...</SDFTraces>
            <SDFTags>...</SDFTags>
        </SDFDebug>
        ...
        <SDFDebug>...</SDFDebug>
    </SDFDebugs>
    
    <SDFNodes> contains the chain of XML representations of the SDFNode(s), with their index and their morphological information (the ids of the governors are taken 
        to be the indexes of the SDFNode).
    <SDFRules> contains the XML representations of the SDFRule(s) that has been used on at least one SDFNode.
    <SDFTraces> contain the SDFTrace of the execution of (nodes of the) SDFRule on the SDFNode(s). 
    <SDFTags> contains the association between tags, idSDFRule(s) + idInstance(s) of the SDFRule(s) and indexes of the SDFNode(s)
    /**/
    
/********************************************************************************************************************************************************************/    
//  First constructor: used from SDFTagger only
/********************************************************************************************************************************************************************/
    private File logFile = null;
    public SDFLogger(File logFile)throws Exception
    {
        this.logFile=logFile;
        if(logFile!=null)while(logFile.exists())logFile.delete();
    }
    
        //It keeps the correspondence between the id of an SDFRule and the SDFRule itself. We report a single XML representation for every SDFRule
        //used at least once, independently of how many times it has been used (i.e., independently of how many instances of SDFRule were created)
    private Hashtable<String, SDFRule> id2SDFRules = new Hashtable<String, SDFRule>();
    
        //This hashtable keep the correspondence between the current SDFRuleStep(s) and their Element. The hashtable is populated when we start tracing a node.
        //Note that every SDFRuleStep can be executed on multiple SDFNode(s), e.g., when there is maxDistance>1. In such a case, SDFRuleStep is associated to an 
        //Element *twice* (or more). But note also that in the endTracing method, we remove the association from the Hashtable 
    private Hashtable<SDFRule, Element> htSDFRuleSteps2Elements = new Hashtable<SDFRule, Element>();
    
        //The <Step>(s) that are associated with main SDFRule(s) need to be written in the final <SDFDebug>. When we remove them from the hashtable 
        //htSDFRuleSteps2Elements, if they are associated with main SDFRule(s), we store them here.
    private ArrayList<Element> StepsToWriteInSDFDebug = new ArrayList<Element>();
    
        //It creates the association between SDFRuleStep and their Element (on which we'll write the index of the node where it will be executed)
    protected void startTracingOnSDFNode(SDFRule SDFRuleStep, long idInstance, SDFNode SDFNode)
    {
        if(logFile==null)return;
        
        Element Step = new Element("Step");
        htSDFRuleSteps2Elements.put(SDFRuleStep, Step);        
        Step.setAttribute("onSDFNode", ""+SDFNode.index);
        if(stepsIntoStack.isEmpty()==false)stepsIntoStack.get(stepsIntoStack.size()-1).getContent().add(Step);
        
            //SDFRuleStep could be Prev|Next|Governor|Dependent. If className is "SDFRule" we also store SDFRuleStep in idSDFRules2SDFRules 
            //(unless an SDFRuleStep with the same idSDFRules is already there) and we register the id and the priority in the Element.
        String className = SDFRuleStep.getClass().getName();
        if(className.lastIndexOf(".")!=-1)className=className.substring(className.lastIndexOf(".")+1, className.length());
        if(className.lastIndexOf("$")!=-1)className=className.substring(className.lastIndexOf("$")+1, className.length());
        if(className.compareToIgnoreCase("SDFRule")==0)
        {
            Step.setAttribute("id", ""+SDFRuleStep.id);
            Step.setAttribute("idInstance", ""+idInstance);
            Step.setAttribute("priority", ""+SDFRuleStep.priority);
            if(id2SDFRules.get(""+SDFRuleStep.id)==null)id2SDFRules.put(""+SDFRuleStep.id, SDFRuleStep);
        }
    }
    
        //This is used in Prev|Next|Governor|Dependent when there is not precedent|subsequent|governor|dependents.
        //The method startTracingOnNoSDFNodes is *NEVER* executed on an main SDFRule. So this method is basically a copy of the previous one,
        //with the difference than here we don't process the className to see if we're on a main SDFRule.
    protected void startTracingOnNoSDFNodes(SDFRule SDFRuleStep)
    {
        if(logFile==null)return;
        Element Step = new Element("Step");
        htSDFRuleSteps2Elements.put(SDFRuleStep, Step);
        Step.setAttribute("onSDFNode", "none");
        if(stepsIntoStack.isEmpty()==false)stepsIntoStack.get(stepsIntoStack.size()-1).getContent().add(Step);
    }
        
        //Whenever we step into something, e.g. a <prevAlternatives>, <nextAlternative>, etc., we add a level of nesting, represented by an Element with the
        //same name (e.g., Element e = new Element("prevAlternatives"), which is attached to the SDFRuleStep above. 
        //From now onwards, until we step out, we attach all the SDFRuleStep(s) to this step-into Element. Of course, during the execution of the SDFRuleStep(s)
        //we can encounter other step-into Element(s); in such a case, we go down one level (we push that on the stack).
    private ArrayList<Element> stepsIntoStack = new ArrayList<Element>();
    protected void stepInto(SDFRule SDFRuleStep, String stepIntoWhat)
    {
        if(logFile==null)return;
        Element stepIntoWhatXML = new Element(stepIntoWhat);
        stepsIntoStack.add(stepIntoWhatXML);
        htSDFRuleSteps2Elements.get(SDFRuleStep).getContent().add(stepIntoWhatXML);
    }
    
        //Here we just pop the latest Element of the stack.
    protected void stepOut()
    {
        if(logFile==null)return;
        stepsIntoStack.remove(stepsIntoStack.size()-1);
    }
    
        //It writes in the Element associated with the SDFRuleStep whether the step has been satisfied or not by the SDFNode on which the SDFRuleStep was evaluated. 
    protected void endTracing(SDFRule SDFRuleStep, boolean satisfied)
    {
        if(logFile==null)return;
        
        Element Step = htSDFRuleSteps2Elements.get(SDFRuleStep);
        if(satisfied==true)Step.setAttribute("result", "OK");
        else Step.setAttribute("result", "FAILED");
        
        String className = SDFRuleStep.getClass().getName();
        if(className.lastIndexOf(".")!=-1)className=className.substring(className.lastIndexOf(".")+1, className.length());
        if(className.lastIndexOf("$")!=-1)className=className.substring(className.lastIndexOf("$")+1, className.length());
        if(className.compareToIgnoreCase("SDFRule")==0)StepsToWriteInSDFDebug.add(Step);
    }
    
        //This method, called from SDFTagger, at the end of "public ArrayList<SDFTag> tagTrees(ArrayList<SDFDependencyTree> trees) throws Exception",
        //generates a new <SDFDebug> and adds it at the end of <SDFDebugs>.
    protected synchronized void writeInLogFile(SDFNode firstSDFNode, ArrayList<SDFTag> tags)throws Exception
    {
        if(logFile==null)return;
        
            //In case this hashtable is empty ... no need to register the SDFDebug: no SDFRule has been executed!
        if((id2SDFRules==null)||(id2SDFRules.keys().hasMoreElements()==false))return;
                
        try
        {
            Document SDFDebugDoc = new Document();
            SDFDebugDoc.setRootElement(new Element("SDFDebugs"));
            if(logFile.exists())SDFDebugDoc = (Document)new SAXBuilder().build(logFile);
            Element SDFDebugs = SDFDebugDoc.getRootElement();
            if(SDFDebugs.getName().compareToIgnoreCase("SDFDebugs")!=0)throw new Exception();
            
            SDFDebugs.getContent().add(generateNewSDFDebug(firstSDFNode, tags));
            
            XMLOutputter outputter = new XMLOutputter();
            outputter.setFormat(Format.getPrettyFormat().setEncoding("UTF-8"));
            FileOutputStream fos = new FileOutputStream(logFile);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
            outputter.output(SDFDebugDoc, osw);
            osw.close();
            fos.close();
        }catch(Exception e)
        {
            throw new Exception("Unknown format in SDFDebug file "+logFile.getName());
        }
            //We empty the lists of SDFRule(s), SDFTag(s), and Step(s) after we have dropped them in the log file.
        finally{id2SDFRules.clear();htSDFRuleSteps2Elements.clear();StepsToWriteInSDFDebug.clear();}
    }
    
    private Element generateNewSDFDebug(SDFNode firstSDFNode, ArrayList<SDFTag> tags)
    {
        //System.out.println(firstSDFNode.nextSDFNode.SDFHead.getForm());
        //if(firstSDFNode.nextSDFNode.SDFHead.getForm().compareTo("The")==0)
        //    firstSDFNode=firstSDFNode;
        
        Element SDFDebug = new Element("SDFDebug");
        Element SDFNodes = new Element("SDFNodes");
        SDFDebug.getContent().add(SDFNodes);
        Element SDFRules = new Element("SDFRules");
        SDFDebug.getContent().add(SDFRules);
        Element SDFTraces = new Element("SDFTraces");
        SDFDebug.getContent().add(SDFTraces);
        Element SDFTags = new Element("SDFTags");
        SDFDebug.getContent().add(SDFTags);
        
            //(1) <SDFNodes>: We populate <SDFNodes> with the <SDFNode> obtained from the chain starting from firstSDFNode.
            //But first we neeed to create an hashtable of correspondences SDFHead->SDFNode (we need it for writing that the governor of an SDFHead
            //corresponds to the SDFNode index; furthermore, we'll need it also in the next step).
        Hashtable<SDFHead, SDFNode> SDFHeads2SDFNodes = new Hashtable<SDFHead, SDFNode>();
        SDFNode tempSDFNode = firstSDFNode.nextSDFNode;
        while(tempSDFNode!=null){SDFHeads2SDFNodes.put(tempSDFNode.SDFHead, tempSDFNode);tempSDFNode=tempSDFNode.nextSDFNode;}
        
        tempSDFNode = firstSDFNode.nextSDFNode;
        while(tempSDFNode!=null)
        {
            Element SDFNode = new Element("SDFNode");
            SDFNodes.getContent().add(SDFNode);
            
            Element index = new Element("index");
            SDFNode.getContent().add(index);
            index.getContent().add(new Text(""+tempSDFNode.index));
            
            Element Form = new Element("Form");
            SDFNode.getContent().add(Form);
            Form.getContent().add(new Text(tempSDFNode.SDFHead.getForm()));
            
            Element Lemma = new Element("Lemma");
            SDFNode.getContent().add(Lemma);
            Lemma.getContent().add(new Text(tempSDFNode.SDFHead.getLemma()));
            
            Element POS = new Element("POS");
            SDFNode.getContent().add(POS);
            POS.getContent().add(new Text(tempSDFNode.SDFHead.getPOS()));
            
            if((tempSDFNode.SDFHead.getEndOfSentence()!=null)&&(tempSDFNode.SDFHead.getEndOfSentence().length()>0))
            {
                Element endOfSentence = new Element("endOfSentence");
                SDFNode.getContent().add(endOfSentence);
                endOfSentence.getContent().add(new Text(tempSDFNode.SDFHead.getEndOfSentence()));
            }
              
            ArrayList<String> optionalFeatures = tempSDFNode.SDFHead.listOptionalFeatures();
            for(int i=0; i<optionalFeatures.size(); i++)
            {
                Element optionalFeature = new Element(optionalFeatures.get(i));
                SDFNode.getContent().add(optionalFeature);
                optionalFeature.getContent().add(new Text(tempSDFNode.SDFHead.getOptionalFeaturesValue(optionalFeatures.get(i))));
            }
            
            Element Governor = new Element("Governor");
            SDFNode.getContent().add(Governor);
            SDFHead gov = tempSDFNode.SDFHead.getGovernor();
            if(gov==null)Governor.getContent().add(new Text("ROOT"));
            else Governor.getContent().add(new Text(""+SDFHeads2SDFNodes.get(gov).index));
            
            Element Label = new Element("Label");
            SDFNode.getContent().add(Label);
            Label.getContent().add(new Text(tempSDFNode.SDFHead.getLabel()));
            
            tempSDFNode = tempSDFNode.nextSDFNode;
        }
        
            //(2) <SDFTags>: note that we use the Hashtable<SDFHead, SDFNode> SDFHeads2SDFNodes, which has been populated in the previous cycle.
        for(int i=0;i<tags.size();i++)
        {
            Element SDFTag = new Element("SDFTag");
            SDFTags.getContent().add(SDFTag);
            SDFTag.setAttribute("tag", tags.get(i).tag);
            SDFTag.setAttribute("id", ""+tags.get(i).idSDFRule);
            SDFTag.setAttribute("idInstance", ""+tags.get(i).idInstance);
            SDFTag.setAttribute("priority", ""+tags.get(i).priority);
            SDFTag.setAttribute("onSDFNode", ""+SDFHeads2SDFNodes.get(tags.get(i).taggedHead).index);
        }
        
            //(3) <SDFRules>: all SDFRule(s) that have been executed at least once are in idSDFRules2SDFRules, associated with their id (one SDFRule for each id)
        Enumeration keys = id2SDFRules.keys();
        while(keys.hasMoreElements())
        {
            String key = (String)keys.nextElement();
            
            SDFRule SDFRule =  id2SDFRules.get(key);
            try
            {
                SDFRules.getContent().add(SDFRule.buildSDFRuleXML());
            }
            catch(Exception e)
            {
                Element temp = new Element("Exception");
                temp.getContent().add(new Text(e.getClass().getName()+": "+e.getMessage()));
                SDFRules.getContent().add(temp);
            }
        }
        
            //(4) <SDFTraces>:
        for(int i=0;i<StepsToWriteInSDFDebug.size();i++)
            SDFTraces.getContent().add(StepsToWriteInSDFDebug.get(i));
        
        return SDFDebug;
    }
}
