package SDFTagger;

import java.util.*;
import org.jdom2.*;
import SDFTagger.SDFItems.*;

//This class is conceived to be EXTENDABLE. This class only enforces the constraints on the mandatory features (which are private!).
//To add new further checks (on the optional features) the class NEED TO BE EXTENDED!!! And, an object of this extended class has to be inserted in the constructor
//of SDFRule. 
//The extended class must override TWO methods: 
//  - the method "protected SDFNodeConstraints FactorySDFNodeConstraints(){return new SDFNodeConstraints();}"
//  - the method "protected boolean checkOptionalFeatures(Hashtable<String, String> optionalFeatures, SDFHead SDFHead)"
//The latter implements the additional checks on the optional features

public class SDFNodeConstraints 
{
//******************************************************** METHODS THAT NEED TO BE OVERRIDDEN *********************************************************************
    
        //IF THE CLASS IS EXTENDED, THIS METHOD NEEDS TO BE OVERRIDDEN IN ORDER TO INSTANTIATE AND RETURN AN OBJECT IN THE SUB-CLASS!
    protected SDFNodeConstraints FactorySDFNodeConstraints(){return new SDFNodeConstraints();}
    
        //IF THE CLASS IS EXTENDED, THIS METHOD NEEDS TO BE OVERRIDDEN IN ORDER TO PERFORM THE OPTIONAL FEATURES CHECKS!
    protected boolean checkOptionalFeatures(Hashtable<String, String> optionalFeatures, SDFHead SDFHead)
    {
        return true;
    }
    
//************************* ATTRIBUTES: these are the attributes on the SDFNode where the SDFRule (or its nested class) are executed. ************************************
    
        //This is the only protected attribute: we need to read it from SDFRule
    protected ArrayList<String> tags = new ArrayList<String>();
    
    private SDFRule owner = null;
    private ArrayList<SDFHeadConstraints> headAlternatives = new ArrayList<SDFHeadConstraints>();
        
        //private nested class, used to cluster the constraints on a SDFHead (the attribute headAlternatives is an array of these objects).
    private class SDFHeadConstraints
    {
        private String Form = null;
        private String Lemma = null;
        private String POS = null;
        private String endOfSentence = null;

        private ArrayList<String> notForm = new ArrayList<String>();
        private ArrayList<String> notLemma = new ArrayList<String>();
        private ArrayList<String> notPOS = new ArrayList<String>();

            //Bags
        private String Bag = null;
        private ArrayList<String> notInBag = new ArrayList<String>();

            //The following are used to store optional features, e.g. "Gender"->"M", "CatType"->"QUALIF", etc.
        private Hashtable<String, String> optionalFeatures = new Hashtable<String, String>();
        
        private Element buildSDFHeadXML()
        {
            Element head = new Element("head");
            
            if(Form!=null)
            {
                Element Form = new Element("Form");
                head.getContent().add(Form);
                Form.getContent().add(new Text(this.Form));
            }
            
            if(Lemma!=null)
            {
                Element Lemma = new Element("Lemma");
                head.getContent().add(Lemma);
                Lemma.getContent().add(new Text(this.Lemma));
            }
            
            if(POS!=null)
            {
                Element POS = new Element("POS");
                head.getContent().add(POS);
                POS.getContent().add(new Text(this.POS));
            }

            if(endOfSentence!=null)
            {
                Element endOfSentence = new Element("endOfSentence");
                head.getContent().add(endOfSentence);
                endOfSentence.getContent().add(new Text(this.endOfSentence));
            }
            
            for(int i=0;i<notForm.size();i++)
            {
                Element notForm = new Element("notForm");
                head.getContent().add(notForm);
                notForm.getContent().add(new Text(this.notForm.get(i)));
            }
            
            for(int i=0;i<notLemma.size();i++)
            {
                Element notLemma = new Element("notLemma");
                head.getContent().add(notLemma);
                notLemma.getContent().add(new Text(this.notLemma.get(i)));
            }
            
            for(int i=0;i<notPOS.size();i++)
            {
                Element notPOS = new Element("notPOS");
                head.getContent().add(notPOS);
                notPOS.getContent().add(new Text(this.notPOS.get(i)));
            }
            
            if(Bag!=null)
            {
                Element Bag = new Element("Bag");
                head.getContent().add(Bag);
                Bag.setAttribute("name", this.Bag);
            }
            
            for(int i=0;i<notInBag.size();i++)
            {
                Element notInBag = new Element("notInBag");
                head.getContent().add(notInBag);
                notInBag.setAttribute("name", this.notInBag.get(i));
            }
            
            Enumeration en = optionalFeatures.keys();
            while(en.hasMoreElements())
            {
                String key = (String)en.nextElement();
                Element optionalFeature = new Element(key);
                head.getContent().add(optionalFeature);
                optionalFeature.getContent().add(new Text(optionalFeatures.get(key)));
            }
            
            return head;
        }
    }
    
    
//******************************************************** METHODS THAT DO NOT NEED TO BE OVERRIDDEN *********************************************************************
    
        //The next two methods are protected: they need to be called from classes in the same package (and we want to use it from SDFRule). All other methods are private.
        //- doesSDFNodeMatch is used to check whether an SDFNode in input satisfies this SDFNodeConstraints; doesSDFNodeMatch calls both checkMandatoryFeatures (private) 
        //  and checkOptionalFeatures, which always returns true in the basic SDFNodeConstraints class, i.e., it ignores the optional features, but maybe not in the 
        //  extended SDFNodeConstraints class, where checks on optional features may be implemented. doesSDFNodeMatch returns either null (meaning that the SDFNode does
        //  *NOT* satisfy this SDFNodeConstraints) or the array of tags defined in this SDFNodeConstraints. The Tag(s) object are instantiated outside, in that, for 
        //  instantiating Tag(s), we need the rule id, the rule idInstance, and the rule priority.
        //- loadAttributes is used to fill the attributes of an SDFHeadConstraints, after this is instantiated via FactorySDFNodeConstraints. We cannot load the
        //  attributes via a constructor, otherwise we should override also the constructor. Instead, the constructor is empty (so that only FactorySDFNodeConstraints
        //  need to be instantiated: both from the superclass and from the subclass we call an empty constructor), then we fill the attributes with loadAttributes, which
        //  is called within SDFRule and it is common to both the superclass and the subclass
    protected boolean doesSDFNodeMatch(SDFNode SDFNode)
    {
        for(int i=0; i<headAlternatives.size(); i++)
        {
            if(checkMandatoryFeatures(headAlternatives.get(i), SDFNode)==true)
            {
                if(checkOptionalFeatures(headAlternatives.get(i).optionalFeatures, SDFNode.SDFHead)==true)
                {
                    return true;
                }
            }
        }
        
        return false;
    }
   
    protected void loadAttributes(String SDFCodeOfSDFNode, SDFRule owner)
    {
        this.owner = owner;
        
        if(SDFCodeOfSDFNode.indexOf("@")==0)
        {
            while((SDFCodeOfSDFNode.indexOf("@")==0)&&(SDFCodeOfSDFNode.indexOf("@$")!=0))
            {
                tags.add(SDFCodeOfSDFNode.substring(1, SDFCodeOfSDFNode.indexOf("@",1)));
                SDFCodeOfSDFNode=SDFCodeOfSDFNode.substring(SDFCodeOfSDFNode.indexOf("@",1),SDFCodeOfSDFNode.length());
            }
            SDFCodeOfSDFNode = SDFCodeOfSDFNode.substring(1,SDFCodeOfSDFNode.length());
        }
        
            //headAlternatives
        if(SDFCodeOfSDFNode.indexOf("$")==0)
        {
            String headAlternativesCode = SDFCodeOfSDFNode.substring(1, SDFCodeOfSDFNode.length()-1);
            
                //head(s)
            while(headAlternativesCode.isEmpty()==false)
            {
                String headCode = headAlternativesCode.substring(1, headAlternativesCode.indexOf("#",1));
                headAlternativesCode = headAlternativesCode.substring(headAlternativesCode.indexOf("#",1)+1, headAlternativesCode.length());
      
                    //campi di <head> (nello stesso ordine in cui vengono codificati in converterIntoSDFCode) 
                SDFHeadConstraints hc = new SDFHeadConstraints();
                
                if(headCode.indexOf("£a")==0)
                {
                    hc.Bag = headCode.substring(2, headCode.indexOf("£",1));
                    headCode = headCode.substring(headCode.indexOf("£",1)+1, headCode.length());
                }
                while(headCode.indexOf("£b")==0)
                {
                    hc.notInBag.add(headCode.substring(2, headCode.indexOf("£",1)));
                    headCode = headCode.substring(headCode.indexOf("£",1)+1, headCode.length());
                }
                
                if(headCode.indexOf("£c")==0)
                {
                    hc.Form = headCode.substring(2, headCode.indexOf("£",1));
                    headCode = headCode.substring(headCode.indexOf("£",1)+1, headCode.length());
                }
                if(headCode.indexOf("£d")==0)
                {
                    hc.Lemma = headCode.substring(2, headCode.indexOf("£",1));
                    headCode = headCode.substring(headCode.indexOf("£",1)+1, headCode.length());
                }
                if(headCode.indexOf("£e")==0)
                {
                    hc.POS = headCode.substring(2, headCode.indexOf("£",1));
                    headCode = headCode.substring(headCode.indexOf("£",1)+1, headCode.length());
                }
                if(headCode.indexOf("£f")==0)
                {
                    hc.endOfSentence = headCode.substring(2, headCode.indexOf("£",1));
                    headCode = headCode.substring(headCode.indexOf("£",1)+1, headCode.length());
                }
                while(headCode.indexOf("£g")==0)
                {
                    hc.notForm.add(headCode.substring(2, headCode.indexOf("£",1)));
                    headCode = headCode.substring(headCode.indexOf("£",1)+1, headCode.length());
                }
                while(headCode.indexOf("£h")==0)
                {
                    hc.notLemma.add(headCode.substring(2, headCode.indexOf("£",1)));
                    headCode = headCode.substring(headCode.indexOf("£",1)+1, headCode.length());
                }
                while(headCode.indexOf("£i")==0)
                {
                    hc.notPOS.add(headCode.substring(2, headCode.indexOf("£",1)));
                    headCode = headCode.substring(headCode.indexOf("£",1)+1, headCode.length());
                }
                while(headCode.indexOf("£j")==0)
                {
                    String key = headCode.substring(2, headCode.indexOf("£k"));
                    headCode = headCode.substring(headCode.indexOf("£k")+2, headCode.length());
                    String value = headCode.substring(0, headCode.indexOf("£"));
                    headCode = headCode.substring(headCode.indexOf("£")+1, headCode.length());
                    hc.optionalFeatures.put(key, value);
                }
                
                headAlternatives.add(hc);
            }
        }
            
            //We don't allow empty headAlternatives! If we want to assert "any head", we assert <headAlternatives><head></head></headAlternatives>, 
            //where <head> is empty. So, we check if that's the case and in such a case we add an empty HeadConstraint
        if(headAlternatives.isEmpty())headAlternatives.add(new SDFHeadConstraints());
    }

        //private procedure to check mandatory features
        //Note that the second parameter is SDFNode. On the other hand, for optional features the second parameter is SDFHead.
        //That's because among the mandatory features there are also the bags, while there are not for optional ones (bags are defined on the SDFNode)
    private boolean checkMandatoryFeatures(SDFHeadConstraints SDFHeadConstraints, SDFNode SDFNode)
    {
            //Form
        if(SDFHeadConstraints.Form!=null)
        {
            if(SDFHeadConstraints.Form.compareToIgnoreCase(SDFNode.SDFHead.getForm()) != 0) return false;
        }

            //notForm
        for(int i=0; i<SDFHeadConstraints.notForm.size(); i++)
            if(SDFHeadConstraints.notForm.get(i).compareToIgnoreCase(SDFNode.SDFHead.getForm())==0) 
                return false;

            //Lemma
        if(SDFHeadConstraints.Lemma!=null)
        {
            if(SDFHeadConstraints.Lemma.compareToIgnoreCase(SDFNode.SDFHead.getLemma()) != 0) return false;
        }

            //notLemma
        for(int i=0; i<SDFHeadConstraints.notLemma.size(); i++)
            if(SDFHeadConstraints.notLemma.get(i).compareToIgnoreCase(SDFNode.SDFHead.getLemma())==0) 
                return false;

            //POS
        if(SDFHeadConstraints.POS!=null)
        {
            if(SDFHeadConstraints.POS.compareToIgnoreCase(SDFNode.SDFHead.getPOS())!=0) return false;
        }

            //notPOS
        for(int i=0; i<SDFHeadConstraints.notPOS.size(); i++)
            if(SDFHeadConstraints.notPOS.get(i).compareToIgnoreCase(SDFNode.SDFHead.getPOS())==0)
                return false;
    
            //endOfSentence
        if(SDFHeadConstraints.endOfSentence!=null)
        {
            if((SDFHeadConstraints.endOfSentence.compareToIgnoreCase("true")==0)&&(SDFNode.endOfSentence==false))return false;
            if((SDFHeadConstraints.endOfSentence.compareToIgnoreCase("false")==0)&&(SDFNode.endOfSentence==true))return false;
        }
        
            //Bags
        if(SDFHeadConstraints.Bag!=null)
        {
                //we search if the bagsOnForm or bagsOnLemma of the SDFNode contain the one specified in the SDFRule.
            boolean found = false;
            for(int i=0;(SDFNode.bagsOnForm!=null)&&(found==false)&&(i<SDFNode.bagsOnForm.size());i++)
                if(SDFNode.bagsOnForm.get(i).compareToIgnoreCase(SDFHeadConstraints.Bag)==0)
                    found=true;
            for(int i=0;(SDFNode.bagsOnLemma!=null)&&(found==false)&&(i<SDFNode.bagsOnLemma.size());i++)
                if(SDFNode.bagsOnLemma.get(i).compareToIgnoreCase(SDFHeadConstraints.Bag)==0)
                    found=true;
            if(found==false)return false;
        }
        
            //notInBag: the same, but in this case none of the Bag(s) specified in the SDFRule must be included in the ones associated with the SDFNode
        if(SDFHeadConstraints.notInBag!=null)
        {
            boolean found = false;
            for(int j=0; (found==false)&&(j<SDFHeadConstraints.notInBag.size()); j++)
            {
                for(int i=0;(SDFNode.bagsOnForm!=null)&&(found==false)&&(i<SDFNode.bagsOnForm.size());i++)
                    if(SDFNode.bagsOnForm.get(i).compareToIgnoreCase(SDFHeadConstraints.notInBag.get(j))==0)
                        found=true;
                for(int i=0;(SDFNode.bagsOnLemma!=null)&&(found==false)&&(i<SDFNode.bagsOnLemma.size());i++)
                    if(SDFNode.bagsOnLemma.get(i).compareToIgnoreCase(SDFHeadConstraints.notInBag.get(j))==0)
                        found=true;
            }
            if(found==true)return false;
        }

        return true;
    }
    
    protected Element[] buildTagsAndHeadAlternativesXML()
    {
        Element[] ret = new Element[1+tags.size()];
        for(int i=0;i<tags.size();i++)
        {
            ret[i] = new Element("tag");
            ret[i].getContent().add(new Text(tags.get(i)));
        }
                
        ret[ret.length-1] = new Element("headAlternatives");
        for(int i=0;i<headAlternatives.size();i++)ret[ret.length-1].getContent().add(headAlternatives.get(i).buildSDFHeadXML());

        return ret;
    }
}
