package SDFTagger.KBInterface.SDFCodesInterface;

import SDFTagger.*;
import java.util.*;


public class SDFCodeFactory
{
    protected long id=-1;
    protected long priority=-1;
    protected ArrayList<String> tags = new ArrayList<String>();
    protected ArrayList<HeadConstraint> headAlternatives = new ArrayList<HeadConstraint>();
    protected ArrayList<Prev> prevAlternatives = new ArrayList<Prev>();
    protected ArrayList<Next> nextAlternatives = new ArrayList<Next>();
    protected ArrayList<Next> nextStarAlternatives = new ArrayList<Next>();
    protected ArrayList<Prev> prevStarAlternatives = new ArrayList<Prev>();
    protected ArrayList<ArrayList<Dependent>> dependentsAlternatives = new ArrayList<ArrayList<Dependent>>();
    protected ArrayList<Governor> governorAlternatives = new ArrayList<Governor>();
    

    public SDFCodeFactory
    (
        long priority,
        Hashtable<String, String> pairsSlotValue, 
        Hashtable<String, ArrayList<String>> notPairsSlotValues
    )throws Exception
    {
        id = 1;
        this.priority=priority;
        headAlternatives.add(buildSDFHead(pairsSlotValue, notPairsSlotValues));
    }
    
/********************** METHODS TO ADD CONTENT TO A NODE, GIVEN ITS XPath **********************/
    public void addSDFTag(String XPath, String tag)throws Exception
    {
        SDFCodeFactory SDFCodeFactory = lookForNode(XPath);
        SDFCodeFactory.tags.add(tag);
    }
    
    public void addHead(String XPath, int maxDistance, Hashtable<String, String> pairsSlotValue, Hashtable<String, ArrayList<String>> notPairsSlotValues)throws Exception
    {
        if(maxDistance<=0)throw new Exception("maxDistance="+maxDistance+" is not allowed");
        SDFCodeFactory SDFCodeFactory = lookForNode(XPath);
        SDFCodeFactory.headAlternatives.add(buildSDFHead(pairsSlotValue, notPairsSlotValues));
    }
    
    public void addPrev(String XPath, int maxDistance, Hashtable<String, String> pairsSlotValue, Hashtable<String, ArrayList<String>> notPairsSlotValues)throws Exception
    {
        if(maxDistance<=0)throw new Exception("maxDistance="+maxDistance+" is not allowed");
        SDFCodeFactory SDFCodeFactory = lookForNode(XPath);
        SDFCodeFactory.prevAlternatives.add(new Prev(maxDistance, pairsSlotValue, notPairsSlotValues));
    }
    
    public void addNotPrev(String XPath, int maxDistance, Hashtable<String, String> pairsSlotValue, Hashtable<String, ArrayList<String>> notPairsSlotValues)throws Exception
    {
        if(maxDistance<=0)throw new Exception("maxDistance="+maxDistance+" is not allowed");
        SDFCodeFactory SDFCodeFactory = lookForNode(XPath);
        Prev Prev = new Prev(maxDistance, pairsSlotValue, notPairsSlotValues);
        Prev.not = true;
        SDFCodeFactory.prevAlternatives.add(Prev);
    }
    
    public void addPrevStar(String XPath, int maxDistance, Hashtable<String, String> pairsSlotValue, Hashtable<String, ArrayList<String>> notPairsSlotValues)throws Exception
    {
        if(maxDistance<=0)throw new Exception("maxDistance="+maxDistance+" is not allowed");
        SDFCodeFactory SDFCodeFactory = lookForNode(XPath);
        SDFCodeFactory.prevStarAlternatives.add(new Prev(maxDistance, pairsSlotValue, notPairsSlotValues));
    }
    
    public void addNotPrevStar(String XPath, int maxDistance, Hashtable<String, String> pairsSlotValue, Hashtable<String, ArrayList<String>> notPairsSlotValues)throws Exception
    {
        if(maxDistance<=0)throw new Exception("maxDistance="+maxDistance+" is not allowed");
        SDFCodeFactory SDFCodeFactory = lookForNode(XPath);
        Prev Prev = new Prev(maxDistance, pairsSlotValue, notPairsSlotValues);
        Prev.not = true;
        SDFCodeFactory.prevStarAlternatives.add(Prev);
    }

    public void addNext(String XPath, int maxDistance, Hashtable<String, String> pairsSlotValue, Hashtable<String, ArrayList<String>> notPairsSlotValues)throws Exception
    {
        if(maxDistance<=0)throw new Exception("maxDistance="+maxDistance+" is not allowed");
        SDFCodeFactory SDFCodeFactory = lookForNode(XPath);
        SDFCodeFactory.nextAlternatives.add(new Next(maxDistance, pairsSlotValue, notPairsSlotValues));
    }
    
    public void addNotNext(String XPath, int maxDistance, Hashtable<String, String> pairsSlotValue, Hashtable<String, ArrayList<String>> notPairsSlotValues)throws Exception
    {
        if(maxDistance<=0)throw new Exception("maxDistance="+maxDistance+" is not allowed");
        SDFCodeFactory SDFCodeFactory = lookForNode(XPath);
        Next Next = new Next(maxDistance, pairsSlotValue, notPairsSlotValues);
        Next.not = true;
        SDFCodeFactory.nextAlternatives.add(Next);
    }
    
    public void addNextStar(String XPath, int maxDistance, Hashtable<String, String> pairsSlotValue, Hashtable<String, ArrayList<String>> notPairsSlotValues)throws Exception
    {
        if(maxDistance<=0)throw new Exception("maxDistance="+maxDistance+" is not allowed");
        SDFCodeFactory SDFCodeFactory = lookForNode(XPath);
        SDFCodeFactory.nextStarAlternatives.add(new Next(maxDistance, pairsSlotValue, notPairsSlotValues));
    }
    
    public void addNotNextStar(String XPath, int maxDistance, Hashtable<String, String> pairsSlotValue, Hashtable<String, ArrayList<String>> notPairsSlotValues)throws Exception
    {
        if(maxDistance<=0)throw new Exception("maxDistance="+maxDistance+" is not allowed");
        SDFCodeFactory SDFCodeFactory = lookForNode(XPath);
        Next Next = new Next(maxDistance, pairsSlotValue, notPairsSlotValues);
        Next.not = true;
        SDFCodeFactory.nextStarAlternatives.add(Next);
    }
    
    public void addGovernor(String XPath, int maxHeight, Hashtable<String, String> pairsSlotValue, Hashtable<String, ArrayList<String>> notPairsSlotValues)throws Exception
    {
        if(maxHeight<=0)throw new Exception("maxHeight="+maxHeight+" is not allowed");
        SDFCodeFactory SDFCodeFactory = lookForNode(XPath);
        SDFCodeFactory.governorAlternatives.add(new Governor(maxHeight, pairsSlotValue, notPairsSlotValues));
    }
    

    public void addDependents(String XPath, int maxDepth, Hashtable<String, String> pairsSlotValue, Hashtable<String, ArrayList<String>> notPairsSlotValues)throws Exception
    {
        if(maxDepth<=0)throw new Exception("maxDepth="+maxDepth+" is not allowed");
        SDFCodeFactory SDFCodeFactory = lookForNode(XPath);
        ArrayList<Dependent> dependents = new ArrayList<Dependent>();
        SDFCodeFactory.dependentsAlternatives.add(dependents);
        dependents.add(new Dependent(maxDepth, pairsSlotValue, notPairsSlotValues));
    }
    


    private HeadConstraint buildSDFHead(Hashtable<String, String> pairsSlotValue, Hashtable<String, ArrayList<String>> notPairsSlotValues)throws Exception
    {

        HeadConstraint HeadConstraint = new HeadConstraint();
        
        for(String slot:pairsSlotValue.keySet())
        {
            String value = pairsSlotValue.get(slot);
            if(slot.compareToIgnoreCase("Form")==0)HeadConstraint.Form=value;
            else if(slot.compareToIgnoreCase("Lemma")==0)HeadConstraint.Lemma=value;
            else if(slot.compareToIgnoreCase("POS")==0)HeadConstraint.POS=value;
            else if(slot.compareToIgnoreCase("Bag")==0)HeadConstraint.Bag=value;
            else if(slot.compareToIgnoreCase("endOfSentence")==0)
            {
                if((value.compareToIgnoreCase("true")==0)||(value.compareToIgnoreCase("false")==0))HeadConstraint.endOfSentence=value;
                else throw new Exception("The \"endOfSentence\" slot only accepts \"true\" or \"false\" as value.");
            }

            else HeadConstraint.optionalFeaturesString.put(slot, value);
        }
        

        for(String slot:notPairsSlotValues.keySet())
        {
            ArrayList<String> values = notPairsSlotValues.get(slot);
            if(slot.compareToIgnoreCase("notForm")==0)for(String value:values)HeadConstraint.notForm.add(value);
            else if(slot.compareToIgnoreCase("notLemma")==0)for(String value:values)HeadConstraint.notLemma.add(value);
            else if(slot.compareToIgnoreCase("notPOS")==0)for(String value:values)HeadConstraint.notPOS.add(value);
            else if(slot.compareToIgnoreCase("notInBag")==0)for(String value:values)HeadConstraint.notInBag.add(value);
            else throw new Exception("The \""+slot+"\" slot is not an acceptable not-slot.");
        }
        
        return HeadConstraint;
    }
    
    private SDFCodeFactory lookForNode(String XPath)throws Exception
    {
        try
        {
            if((XPath.indexOf("/")==-1)&&(XPath.compareToIgnoreCase("SDFRule")==0))return this;
            
            String SDFRule = XPath.substring(0, XPath.indexOf("/"));
            if(SDFRule.compareToIgnoreCase("SDFRule")!=0)throw new Exception();
            XPath = XPath.substring(XPath.indexOf("/")+1, XPath.length());
            
            return lookForNodeRecursive(this, XPath);
        }
        catch(Exception e){throw new Exception("Incorrect XPath: \""+XPath+"\" :"+e.getMessage());}
    }
    
    private SDFCodeFactory lookForNodeRecursive(SDFCodeFactory node, String XPath)throws Exception
    {
        XPath = XPath.trim();
        String nodeName = XPath;
        if(XPath.indexOf("/")!=-1)
        {
            nodeName = XPath.substring(0, XPath.indexOf("/")).trim();
            XPath = XPath.substring(XPath.indexOf("/")+1, XPath.length()).trim();
        }else XPath="";
        
        SDFCodeFactory nextRecursionNode = null;
        
            
        int openSP = nodeName.lastIndexOf("[");
        int closedSP = nodeName.lastIndexOf("]");
        if((openSP<0)||(closedSP<0)||(openSP>=closedSP))throw new Exception(nodeName+" does not have a proper [..]");
        int index = Integer.parseInt(nodeName.substring(openSP+1, closedSP).trim())-1;
        if(index<0)throw new Exception("Index "+(index+1)+" does not exist in \""+XPath+"/"+nodeName+"["+(index+1)+"]\"");
        nodeName = nodeName.substring(0, nodeName.lastIndexOf("[")).trim();
        
        if(nodeName.compareToIgnoreCase("Prev")==0)
        {
            if(index>=node.prevAlternatives.size())throw new Exception("Index "+(index+1)+" does not exist in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\"");
            nextRecursionNode = node.prevAlternatives.get(index);
            if(((Prev)nextRecursionNode).not==true)throw new Exception("The element at index "+(index+1)+" in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\" is notPrev, not Prev.");
        }
        else if(nodeName.compareToIgnoreCase("notPrev")==0)
        {
            if(index>=node.prevAlternatives.size())throw new Exception("Index "+(index+1)+" does not exist in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\"");
            nextRecursionNode = node.prevAlternatives.get(index);
            if(((Prev)nextRecursionNode).not==false)throw new Exception("The element at index "+(index+1)+" in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\" is Prev, not notPrev.");
        }
        else if(nodeName.compareToIgnoreCase("PrevStar")==0)
        {
            if(index>=node.prevStarAlternatives.size())throw new Exception("Index "+(index+1)+" does not exist in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\"");
            nextRecursionNode = node.prevStarAlternatives.get(index);
            if(((Prev)nextRecursionNode).not==true)throw new Exception("The element at index "+(index+1)+" in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\" is notPrevStar, not PrevStar.");
        }
        else if(nodeName.compareToIgnoreCase("notPrevStar")==0)
        {
            if(index>=node.prevStarAlternatives.size())throw new Exception("Index "+(index+1)+" does not exist in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\"");
            nextRecursionNode = node.prevStarAlternatives.get(index);
            if(((Prev)nextRecursionNode).not==false)throw new Exception("The element at index "+(index+1)+" in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\" is PrevStar, not notPrevStar.");
        }        
        else if(nodeName.compareToIgnoreCase("Next")==0)
        {
            if(index>=node.nextAlternatives.size())throw new Exception("Index "+(index+1)+" does not exist in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\"");
            nextRecursionNode = node.nextAlternatives.get(index);
            if(((Next)nextRecursionNode).not==true)throw new Exception("The element at index "+(index+1)+" in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\" is notNext, not Next.");
        }
        else if(nodeName.compareToIgnoreCase("notNext")==0)
        {
            if(index>=node.nextAlternatives.size())throw new Exception("Index "+(index+1)+" does not exist in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\"");
            nextRecursionNode = node.nextAlternatives.get(index);
            if(((Next)nextRecursionNode).not==false)throw new Exception("The element at index "+(index+1)+" in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\" is Next, not notNext.");
        }
        else if(nodeName.compareToIgnoreCase("NextStar")==0)
        {
            if(index>=node.nextStarAlternatives.size())throw new Exception("Index "+(index+1)+" does not exist in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\"");
            nextRecursionNode = node.nextStarAlternatives.get(index);
            if(((Next)nextRecursionNode).not==true)throw new Exception("The element at index "+(index+1)+" in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\" is notNextStar, not NextStar.");
        }
        else if(nodeName.compareToIgnoreCase("NextStar")==0)
        {
            if(index>=node.nextStarAlternatives.size())throw new Exception("Index "+(index+1)+" does not exist in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\"");
            nextRecursionNode = node.nextStarAlternatives.get(index);
            if(((Next)nextRecursionNode).not==false)throw new Exception("The element at index "+(index+1)+" in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\" is NextStar, not notNextStar.");
        }
        else if(nodeName.compareToIgnoreCase("Governor")==0)
        {
            if(index>=node.governorAlternatives.size())throw new Exception("Index "+(index+1)+" does not exist in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\"");
            nextRecursionNode = node.governorAlternatives.get(index);
            if(((Governor)nextRecursionNode).not==true)throw new Exception("The element at index "+(index+1)+" in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\" is notGovernor, not Governor");
        }
        else if(nodeName.compareToIgnoreCase("notGovernor")==0)
        {
            if(index>=node.governorAlternatives.size())throw new Exception("Index "+(index+1)+" does not exist in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\"");
            nextRecursionNode = node.governorAlternatives.get(index);
            if(((Governor)nextRecursionNode).not==false)throw new Exception("The element at index "+(index+1)+" in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\" is Governor, not notGovernor");
        }
        else if(nodeName.compareToIgnoreCase("Dependents")==0)
        {
            if(index>=node.dependentsAlternatives.size())throw new Exception("Index "+(index+1)+" does not exist in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\""); 
            
            ArrayList<Dependent> dependents = node.dependentsAlternatives.get(index); 

                
            if(XPath.indexOf("Dependent")!=0)throw new Exception("Dependents without Dependent in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\""); 
            nodeName = XPath;
            if(XPath.indexOf("/")!=-1)
            {
                nodeName = XPath.substring(0, XPath.indexOf("/")).trim();
                XPath = XPath.substring(XPath.indexOf("/")+1, XPath.length()).trim();
            }
            
            if((nodeName.indexOf("Dependent")!=0)&&(nodeName.indexOf("notDependent")!=0))
                throw new Exception("Dependents without Dependent or notDependent in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\""); 
            openSP = nodeName.lastIndexOf("[");
            closedSP = nodeName.lastIndexOf("]");
            if((openSP<0)||(closedSP<0)||(openSP>=closedSP))throw new Exception(nodeName+" does not have a proper [..]");
            index = Integer.parseInt(nodeName.substring(openSP+1, closedSP).trim())-1;
            if(index<0)throw new Exception("Index "+(index+1)+" does not exist in \""+XPath+"/"+nodeName+"["+(index+1)+"]\"");
            if(index>=dependents.size())throw new Exception("Index "+(index+1)+" does not exist in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\"");
            
            nodeName = nodeName.substring(0, nodeName.lastIndexOf("[")).trim();
            nextRecursionNode = dependents.get(index);
            if((nodeName.compareToIgnoreCase("Dependent")==0)&&(((Dependent)nextRecursionNode).not==true))
                throw new Exception("The element at index "+(index+1)+" in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\" is notDependent, not Dependent");
            if((nodeName.compareToIgnoreCase("Dependent")==0)&&(((Dependent)nextRecursionNode).not==false))
                throw new Exception("The element at index "+(index+1)+" in \"/"+nodeName+"["+(index+1)+"]/"+XPath+"\" is Dependent, not notDependent");
        }
        
            
        if(XPath.isEmpty())return nextRecursionNode;
        

        return lookForNodeRecursive(nextRecursionNode, XPath);
    }
    
  
    protected class HeadConstraint
    {
        protected String Form = null;
        protected String Lemma = null;
        protected String POS = null;
        protected String endOfSentence = null;
        protected ArrayList<String> notForm = new ArrayList<String>();
        protected ArrayList<String> notLemma = new ArrayList<String>();
        protected ArrayList<String> notPOS = new ArrayList<String>();
        protected Hashtable<String, String> optionalFeaturesString = new Hashtable<String, String>();
        

        protected Hashtable<String, String> optionalFeaturesSDFHead = new Hashtable<String, String>();
        
        public String Bag = null;
        public ArrayList<String> notInBag = new ArrayList<String>();
    }
                    
    protected class Prev extends SDFCodeFactory
    {
        protected int maxDistance = -1;
        protected boolean not = false;
        
        public Prev(int maxDistance, Hashtable<String, String> pairsSlotValue, Hashtable<String, ArrayList<String>> notPairsSlotValues)throws Exception
        {

            super(0, pairsSlotValue, notPairsSlotValues);
            this.maxDistance=maxDistance;
        }
        
        public String getSDFCode()throws Exception
        {
            String ret = "";

            if(not==true)ret="£D";

            return ret + "£C" + maxDistance + "£C" + super.getSDFCode();
        }
    }

    protected class Next extends SDFCodeFactory
    {
        protected int maxDistance = -1;
        protected boolean not = false;
        
        public Next(int maxDistance, Hashtable<String, String> pairsSlotValue, Hashtable<String, ArrayList<String>> notPairsSlotValues)throws Exception
        {
            super(0, pairsSlotValue, notPairsSlotValues);
            this.maxDistance=maxDistance;
        }
        
        public String getSDFCode()throws Exception
        {
            String ret = "";

            if(not==true)ret="£D";
            return ret + "£C" + maxDistance + "£C" + super.getSDFCode();
        }
    }
    
    protected class Governor extends SDFCodeFactory
    {
        protected int maxHeight = -1;
        protected boolean not = false;
        protected ArrayList<String> labelAlternatives = new ArrayList<String>();
        
        public Governor(int maxHeight, Hashtable<String, String> pairsSlotValue, Hashtable<String, ArrayList<String>> notPairsSlotValues)throws Exception
        {
            super(0, pairsSlotValue, notPairsSlotValues);
            this.maxHeight=maxHeight;
        }

        public String getSDFCode()throws Exception
        {
            String ret = "";
            
            if(not==true)ret="£D";
            
            if(labelAlternatives.size()>0)ret=ret+"£E";
            for(int i=0;i<labelAlternatives.size();i++)ret=ret+labelAlternatives.get(i)+"£E";
            
            return ret + "£C" + maxHeight + "£C" + super.getSDFCode();
        }
    }
    
    protected class Dependent extends SDFCodeFactory
    {
        protected int maxDepth = -1;
        protected boolean not = false;
        protected ArrayList<String> labelAlternatives = new ArrayList<String>();

        public Dependent(int maxDepth, Hashtable<String, String> pairsSlotValue, Hashtable<String, ArrayList<String>> notPairsSlotValues)throws Exception
        {
            super(0, pairsSlotValue, notPairsSlotValues);
            this.maxDepth=maxDepth;
        }

        public String getSDFCode()throws Exception
        {
            String ret = "";
            

            if(not==true)ret="£D";
            

            if(labelAlternatives.size()>0)ret=ret+"£E";
            for(int i=0;i<labelAlternatives.size();i++)ret=ret+labelAlternatives.get(i)+"£E";
            

            return ret + "£C" + maxDepth + "£C" + super.getSDFCode();
        }
    }

    private String SDFCode = null;
    public String getSDFCode()throws Exception
    {
        if(SDFCode!=null)return SDFCode;
        for(Prev Prev:prevAlternatives)Prev.getSDFCode();
        for(Next Next:nextAlternatives)Next.getSDFCode();
        for(Prev Prev:prevStarAlternatives)Prev.getSDFCode();
        for(Next Next:nextStarAlternatives)Next.getSDFCode();
        for(Governor Governor:governorAlternatives)Governor.getSDFCode();
        for(ArrayList<Dependent> dependents:dependentsAlternatives)for(Dependent Dependent:dependents)Dependent.getSDFCode();
        SDFCode=buildSDFCode();
        return SDFCode;
    }
    
    private class SDFTaggerConfigEX extends SDFTaggerConfig{public SDFTaggerConfigEX()throws Exception{}}
    public String getXMLString()throws Exception
    {
        org.jdom2.Element Element = new SDFRule(getSDFCode(), new SDFNodeConstraints(), new SDFTagger(new SDFTaggerConfigEX())).buildSDFRuleXML();
        org.jdom2.Document doc = new org.jdom2.Document(Element);
        org.jdom2.output.XMLOutputter outputter = new org.jdom2.output.XMLOutputter(org.jdom2.output.Format.getPrettyFormat());
        return outputter.outputString(doc);
    }


    private static int prevStarAlternativesCounter = 1;
    private static int prevAlternativesCounter = 1;
    private static int nextStarAlternativesCounter = 1;
    private static int nextAlternativesCounter = 1;
    private static int prevCounter = 1;
    private static int nextCounter = 1;
    private static int dependentsAlternativesCounter = 1;
    private static int dependentsCounter = 1;
    private static int dependentCounter = 1;
    private static int governorAlternativesCounter = 1;
    private static int governorCounter = 1;
    private String buildSDFCode() throws Exception
    {
        if(id==1)
            id=id;
        
        String ret = "";

        if(this.getClass().getName().indexOf("$")==-1)
            ret=id+"£"+priority+"£";
        
        if(tags.size()>0)ret=ret+"@";
        for(int i=0;i<tags.size();i++)ret=ret+tags.get(i)+"@";
        
        if(headAlternatives.size()>0)
        {
            ret = ret + "$";
                    
            for(int i=0; i<headAlternatives.size(); i++)
            {
                ret = ret + "#";
                
                HeadConstraint hc = (HeadConstraint)headAlternatives.get(i);
                if(hc.Bag!=null)ret = ret + "£a" + hc.Bag + "£";
                if(hc.notInBag.isEmpty()==false)for(int j=0;j<hc.notInBag.size();j++)ret=ret+"£b"+hc.notInBag.get(j)+"£";
                
                if((hc.Form!=null)&&(hc.Form.isEmpty()==false)) ret = ret + "£c" + hc.Form + "£";
                if((hc.Lemma!=null)&&(hc.Lemma.isEmpty()==false)) ret = ret + "£d" + hc.Lemma + "£";
                if((hc.POS!=null)&&(hc.POS.isEmpty()==false)) ret = ret + "£e" + hc.POS + "£";
                if((hc.endOfSentence!=null)&&(hc.endOfSentence.isEmpty()==false)) ret = ret + "£f" + hc.endOfSentence + "£";
                if(hc.notForm.isEmpty()==false)for(int j=0; j<hc.notForm.size(); j++) ret = ret + "£g" + hc.notForm.get(j) + "£";
                if(hc.notLemma.isEmpty()==false)for(int j=0; j<hc.notLemma.size(); j++) ret = ret + "£h" + hc.notLemma.get(j) + "£";
                if(hc.notPOS.isEmpty()==false)for(int j=0; j<hc.notPOS.size(); j++) ret = ret + "£i" + hc.notPOS.get(j) + "£";
                
                Enumeration enOFS = hc.optionalFeaturesString.keys();
                while(enOFS.hasMoreElements())
                {
                    String key = (String)enOFS.nextElement();
                    ret = ret + "£j" + key + "£k" + hc.optionalFeaturesString.get(key) + "£";
                }

                Enumeration enOFSDFHead = hc.optionalFeaturesSDFHead.keys();
                while(enOFSDFHead.hasMoreElements())
                {
                    String key = (String)enOFSDFHead.nextElement();
                    ret = ret + "£l" + key + "£m" + hc.optionalFeaturesSDFHead.get(key) + "£";
                }
                
                ret = ret + "#";
            }
            
            ret = ret + "$";
        }

        if(prevStarAlternatives.isEmpty()==false)
        {
            ret = ret + "ç"+prevStarAlternativesCounter + ",";
            for(int i=0; i<prevStarAlternatives.size(); i++)
            {
                ret = ret + "+" + prevCounter + ",";
                ret = ret + ((Prev)prevStarAlternatives.get(i)).getSDFCode() + "+" + prevCounter + ",";
                prevCounter++;
            }
            ret = ret + "ç"+prevStarAlternativesCounter + ",";
            prevStarAlternativesCounter++;
        }

        if(prevAlternatives.isEmpty()==false)
        {
            ret = ret + "%"+prevAlternativesCounter + ",";
            for(int i=0; i<prevAlternatives.size(); i++)
            {
                ret = ret + "+" + prevCounter + ",";
                ret = ret + ((Prev)prevAlternatives.get(i)).getSDFCode() + "+" + prevCounter + ",";
                prevCounter++;
            }
            ret = ret + "%"+prevAlternativesCounter + ",";
            prevAlternativesCounter++;
        }
        
        if(nextStarAlternatives.isEmpty()==false)
        {
            ret = ret + "?"+nextStarAlternativesCounter + ",";
            for(int i=0; i<nextStarAlternatives.size(); i++)
            {
                ret = ret + "-" + nextCounter + ",";
                ret = ret + ((Next)nextStarAlternatives.get(i)).getSDFCode() + "-" + nextCounter + ",";
                nextCounter++;
            }
            ret = ret + "?"+nextStarAlternativesCounter + ",";
            nextStarAlternativesCounter++;
        }
        
        if(nextAlternatives.isEmpty()==false)
        {
            ret = ret + "!"+nextAlternativesCounter + ",";
            for(int i=0; i<nextAlternatives.size(); i++)
            {
                ret = ret + "-" + nextCounter + ",";
                ret = ret + ((Next)nextAlternatives.get(i)).getSDFCode() + "-" + nextCounter + ",";
                nextCounter++;
            }
            ret = ret + "!"+nextAlternativesCounter + ",";
            nextAlternativesCounter++;
        }
        
        if(governorAlternatives.isEmpty()==false)
        {
            ret = ret + "&"+governorAlternativesCounter + ",";
            for(int i=0; i<governorAlternatives.size(); i++)
            {
                ret = ret + "=" + governorCounter + ",";
                ret = ret + ((Governor)governorAlternatives.get(i)).getSDFCode() + "=" + governorCounter + ",";
                governorCounter++;
            }
            ret = ret + "&"+governorAlternativesCounter + ",";
            governorAlternativesCounter++;
        }
        
        if(dependentsAlternatives.isEmpty()==false)
        {
            ret = ret + ";"+dependentsAlternativesCounter + ",";
            for(int i=0; i<dependentsAlternatives.size(); i++)
            {
                ArrayList<Dependent> dependents = dependentsAlternatives.get(i);
                
                ret = ret + "."+dependentsCounter + ",";
                for(int j=0; j<dependents.size(); j++)
                {
                    ret = ret + "*"+dependentCounter + ",";
                    Dependent dep = (Dependent)dependents.get(j);
                    ret = ret + dep.getSDFCode();
                    ret = ret + "*"+dependentCounter + ",";
                    dependentCounter++;
                }
                ret = ret + "."+dependentsCounter + ",";
                dependentsCounter++;
            }
            ret = ret + ";"+dependentsAlternativesCounter + ",";
            dependentsAlternativesCounter++;
        }
        
        return ret;
    }
}