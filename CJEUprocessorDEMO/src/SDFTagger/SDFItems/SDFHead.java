package SDFTagger.SDFItems;
import java.util.*;

public class SDFHead
{
        //There are two kind of features (depending on their value): either String value or SDFHead value. 
    
        //There are six mandatory features; all the others are optional. If a mandatory feature is missing, the SDFHead cannot be instantiated. 
        //All mandatory features have String value but 'Governor'; this feature has SDFHead value, e.g. "<Governor>eId:15</Governor>".
        //NB. endOfSentence has two possible values: the String(s) "true" and "false"; we don't represent it as a boolean, because it could be "unknown" 
        //(we need a third value)
    protected String Form = null;//XML TAG -> "<Form>"
    protected String Lemma = null;//XML TAG -> "<Lemma>"
    protected String POS = null;//XML TAG -> "<POS>"
    protected String endOfSentence = null;//XML TAG -> "<endOfSentence>"
    protected SDFHead Governor = null;//XML TAG -> "<Governor>"
    protected String Label = null;//XML TAG -> "<Label>"
    protected ArrayList<SDFHead> dependents = new ArrayList<SDFHead>();//no XML tags: this array is filled by looking at the <Governor> XML tags of the dependents.
    
        //The following are used to store optional features, e.g. "Gender"->"M", "CatType"->"QUALIF", etc.
    protected Hashtable<String,String> optionalFeatures = new Hashtable<String,String>();
    
        //The value of each mandatory feature can be read, but it can be written only in the constructor.
    public SDFHead(String Form, String Lemma, String POS, String endOfSentence, SDFHead Governor, String Label)
    {
        this.Form = Form;
        this.Lemma = Lemma;
        this.POS = POS;
        this.endOfSentence = endOfSentence;
        this.Governor = Governor;
        this.Label = Label;
        if((Governor!=null)&&(Governor.dependents.indexOf(this)==-1))Governor.dependents.add(this);
    }
    
    public void setOptionalFeatures(String feature, String value)
    {
            //if the feature is already present, we remove it (the new one overrides it)
        if(optionalFeatures.get(feature)!=null)optionalFeatures.remove(feature);
        optionalFeatures.put(feature, value);
    }
    
    public String getForm()
    {
        return Form;
    }

    public String getLemma()
    {
        return Lemma;
    }
    
    public String getPOS()
    {
        return POS;
    }
    
    public String getEndOfSentence()
    {
        return endOfSentence;
    }
    
    public SDFHead getGovernor()
    {
        return Governor;
    }
   
    public SDFHead[] getDependents()
    {
        SDFHead[] ret = new SDFHead[dependents.size()];
        for(int i=0; i<dependents.size(); i++) ret[i] = dependents.get(i);
        return ret;
    }
    
    public String getLabel()
    {
        return Label;
    }
    
    public ArrayList<String> listOptionalFeatures()
    {
        ArrayList<String> ret = new ArrayList<String>();
        Enumeration en = optionalFeatures.keys();
        while(en.hasMoreElements())ret.add((String)en.nextElement());
        return ret;
    }
    
    public String getOptionalFeaturesValue(String feature){return optionalFeatures.get(feature);}
}

