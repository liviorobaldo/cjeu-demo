package SDFTagger.KBInterface.XMLFilesInterface;

import java.util.*;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import java.io.*;
import SDFTagger.SDFItems.*;
import SDFTagger.KBInterface.*;
import SDFTagger.KBInterface.XMLFilesInterface.XML2SDFCodesCompiler.*;

public class XMLFilesManager implements KBInterface
{
        //The bags are loaded here in these Hashtable(s)
    protected Hashtable<String, ArrayList<String>> allFormsToBags = new Hashtable<String,ArrayList<String>>();
    protected Hashtable<String, ArrayList<String>> allLemmasToBags = new Hashtable<String,ArrayList<String>>();
    
    /**
        On the other hand, in these ArrayList(s), we load the compiled SDFRules. The compiled SDFRules are divided in three sets: 
        (1) If there is a <Form> in <head>, it indexes it in rulesFormIndex, on alphabetical order on the Form
        (2) If (1) does not hold and there is a <Lemma> in <head>, it indexes it in rulesLemmaIndex, on alphabetical order on the Lemma
        (3) If (1) and (2) do not hold and there is a <POS> in <head>, it indexes it in rulesPOSIndex, on alphabetical order on the POS
        (4) If none of the previous ones, it stores the rule in rulesNotAssociatedWithFormsLemmasAndPOSs (without indexing)
        Thus, we can quickly find the rules via binary search on the form, the lemma, and the POS. 

        NOTE: the SDFRule(s) indexed on Form, Lemma, and POS will be searched, via binary search, on Head(s) having that Form, Lemma, or POS
        On the other hand, the SDFRule(s) in (4) are *always* associated to *all* SDFHead(s). 

        NOTE(2): In case an SDFRule has <headAlternatives> belonging to both classes, e.g.,
        <headAlternatives>
            <head><Form>xxx</Form></head>
            <head><POS>yyy</POS></head>
        </headAlternatives>
        this SDFRule is indexed *ONLY* on the Form (read above: "(2) if (1) does not hold ..."). 
        In fact, it is useless to execute that rule on SDFHead(s) having the yyy as POS if they don't also have xxx as Form
    /**/
    
    protected ArrayList<String> rulesFormIndex = new ArrayList<String>();
    protected ArrayList<ArrayList<String>> rulesForm = new ArrayList<ArrayList<String>>();
    protected ArrayList<String> rulesLemmaIndex = new ArrayList<String>();
    protected ArrayList<ArrayList<String>> rulesLemma = new ArrayList<ArrayList<String>>();
    protected ArrayList<String> rulesPOSIndex = new ArrayList<String>();
    protected ArrayList<ArrayList<String>> rulesPOS = new ArrayList<ArrayList<String>>();
    protected ArrayList<String> rulesNotAssociatedWithFormsLemmasAndPOSs = new ArrayList<String>();
    
/************************************ CONSTRUCTOR: IT BUILDS THE Hashtable(s) AND THE ArrayList(s) ABOVE ************************************/
    public XMLFilesManager
    (
            //for the bags ...
        File rootDirectoryBags, String[] localPathsBags,
            //for the XML files
        File rootDirectoryXmlSDFRules, File rootDirectoryCompiledSDFRules, String[] localPathsSDFRules
    ) throws Exception
    {
            //We first check if the files exists. If not, an exception is raised.
        for(int i=0;i<localPathsBags.length;i++)
            if(new File(rootDirectoryBags+"/"+localPathsBags[i]).exists()==false)
                throw new Exception("File \""+new File(rootDirectoryBags+"/"+localPathsBags[i]).getCanonicalPath()+"\" does not exist");
        for(int i=0;i<localPathsSDFRules.length;i++)
            if(new File(rootDirectoryXmlSDFRules+"\\"+localPathsSDFRules[i]).exists()==false)
                throw new Exception("File \""+new File(rootDirectoryXmlSDFRules+"/"+localPathsSDFRules[i]).getCanonicalPath()+"\" does not exist");
        
        loadBags(rootDirectoryBags, localPathsBags);
        
            //We check whether there are updated SDFRule(s), then we load them in the attributes 
            //rulesFormIndex, rulesForm, rulesLemmaIndex, rulesLemma, and rulesNotAssociatedWithFormsAndLemmas
        checkUpdatesXML.updateCompiledKB(rootDirectoryXmlSDFRules, rootDirectoryCompiledSDFRules, localPathsSDFRules);
        
            //We load the SDFRules within the attributes rulesFormIndex, rulesForm, rulesLemmaIndex, rulesLemma, and rulesNotAssociatedWithFormsAndLemmas
        loadCompiledSDFRules(rootDirectoryCompiledSDFRules, localPathsSDFRules);
    }

/************** METHODS OF THE interface KBInterface: populate the SDFNode(s) with the bags and retrieve the SDFCodes of the SDFHead of the SDFNode **************/

        //Each SDFNode in a chain of SDFNode(s) has an ArrayList<String> for the bags on its Form and an ArrayList<String> for the bags on its Lemma.
        //The values of these bags are in the KBInterface specified within the SDFTaggerConfig of the SDFTagger (better: the id of the bags, these are 
        //those included in allFormsToBags and allLemmasToBags).
        //We copy these values in each SDFNode, in order to enable them the checking of the constraints between the node of the SDFRule(s) and the SDFNode.
        //Of course, if the Form (or the Lemma) is not defined in the bags of the KBInterface, bagsOnForm (or bagsOnLemma) are returned as they are.
    public void fillBagsOfSDFNode(String Form, String Lemma, ArrayList<String> bagsOnForm, ArrayList<String> bagsOnLemma)throws Exception
    {
        ArrayList<String> temp = allFormsToBags.get(Form.toLowerCase());
        for(int i=0;(temp!=null)&&(i<temp.size());i++)bagsOnForm.add(temp.get(i));
        temp = allLemmasToBags.get(Lemma.toLowerCase());
        for(int i=0;(temp!=null)&&(i<temp.size());i++)bagsOnLemma.add(temp.get(i));
    }
    
        //This method retrieves the SDFCode(s) and return them in output
    public ArrayList<String> retrieveSDFCodes(SDFHead SDFHead)throws Exception
    {
        String Form = SDFHead.getForm().toLowerCase();
        String Lemma = SDFHead.getLemma().toLowerCase();
        String POS = SDFHead.getPOS().toLowerCase();
                
        ArrayList<String> ret = new ArrayList<String>();
        
            //binary search on the Form
        int a = 0;
        int b = rulesFormIndex.size()-1;
        while(b>=a)
        {
            int m = (a+b)/2;
            if(rulesFormIndex.get(m).compareToIgnoreCase(Form)==0)
            {
                ArrayList<String> rules = rulesForm.get(m);
                for(int i=0;i<rules.size();i++)ret.add(rules.get(i));
                break;//we found the SDFRule(s) of the Form: we add them.
            }
            if((m==a)&&(m==b))break;//the Form does not have any SDFRule associated with.
            
                //binary search: we look for the place of key in the right half of the array
            if(rulesFormIndex.get(m).compareToIgnoreCase(Form)<0)a=m+1;
                //if m==a, we set b==a==m
            else if(m==a)b=a;else b=m-1;
        }
        
            //binary search on the Lemma (the rules in rulesLemma are dijoint with rulesForm)
        a = 0;
        b = rulesLemmaIndex.size()-1;
        while(b>=a)
        {
            int m = (a+b)/2;
            if(rulesLemmaIndex.get(m).compareToIgnoreCase(Lemma)==0)
            {
                ArrayList<String> rules = rulesLemma.get(m);
                for(int i=0;i<rules.size();i++)ret.add(rules.get(i));
                break;//we found the SDFRule(s) of the Lemma: we add them.
            }
            if((m==a)&&(m==b))break;//the Lemma does not have any SDFRule associated with.
            
                //binary search: we look for the place of key in the right half of the array
            if(rulesLemmaIndex.get(m).compareToIgnoreCase(Lemma)<0)a=m+1;
                //if m==a, we set b==a==m
            else if(m==a)b=a;else b=m-1;
        }
        
            //binary search on the POS (the rules in rulesPOS are dijoint with rulesLemma)
        a = 0;
        b = rulesPOSIndex.size()-1;
        while(b>=a)
        {
            int m = (a+b)/2;
            if(rulesPOSIndex.get(m).compareToIgnoreCase(POS)==0)
            {
                ArrayList<String> rules = rulesPOS.get(m);
                for(int i=0;i<rules.size();i++)ret.add(rules.get(i));
                break;//we found the SDFRule(s) of the POS: we add them.
            }
            if((m==a)&&(m==b))break;//the POS does not have any SDFRule associated with.
            
                //binary search: we look for the place of key in the right half of the array
            if(rulesPOSIndex.get(m).compareToIgnoreCase(POS)<0)a=m+1;
                //if m==a, we set b==a==m
            else if(m==a)b=a;else b=m-1;
        }
        
            //these are added for every SDFNode
        for(int i=0;i<rulesNotAssociatedWithFormsLemmasAndPOSs.size();i++)ret.add(rulesNotAssociatedWithFormsLemmasAndPOSs.get(i));
    
        return ret;
    }
        
    
/************************************************** PRIVATE UTILITIES TO LOAD THE PROTECTED ATTRIBUTES: ***************************************************/
/************** - allFormsToBags, allLemmasToBags                                                                                                **********/
/************** - rulesFormIndex, rulesForm, rulesLemmaIndex, rulesLemma, rulesPOSIndex, rulesPOS, and rulesNotAssociatedWithFormsLemmasAndPOSs  **********/
/**********************************************************************************************************************************************************/
    
        //This methods fills the attributes allFormsToBags and allLemmasToBags.
    private void loadBags(File rootDirectoryBags, String[] localPathsBags) throws Exception
    {
        for(int i=0;i<localPathsBags.length;i++)
        {
            File file = new File(rootDirectoryBags.getAbsolutePath()+"/"+localPathsBags[i]);            
            if(file.exists()==false)throw new Exception("The file "+file.getAbsolutePath()+" does not exist");
            try
            {
                Document document = (Document) new SAXBuilder().build(file);
                Element Bags = document.getRootElement();
                for(int j=0; j<Bags.getChildren().size(); j++)
                {
                    if(!(Bags.getChildren().get(j) instanceof Element)) continue;
                    
                    Element Bag = (Element)Bags.getChildren().get(j);
                        //faccio 'sto giro solo per controllare che sia effettivamente un numero...
                    String name = Bag.getAttributeValue("name");
                    String tempType = Bag.getAttributeValue("type");
                    
                    for(int k=0; k<Bag.getChildren().size(); k++)
                    {
                        if(!(Bag.getChildren().get(k) instanceof Element)) continue;
                        if(((Element)Bag.getChildren().get(k)).getName().compareToIgnoreCase("instance")!=0) continue;
                        String instance = ((Text)((Element)Bag.getChildren().get(k)).getContent().get(0)).getText().toLowerCase();
                        
                        //System.out.println("Loading Bag "+id+", instance: "+instance);
                        
                        if(instance.compareToIgnoreCase("dino")==0)
                            instance=instance;

                        ArrayList<String> bags=null;
                        if(tempType.compareToIgnoreCase("Form")==0)
                        {
                            bags = allFormsToBags.get(instance);
                            if(bags==null){bags=new ArrayList<String>();allFormsToBags.put(instance, bags);}
                        }
                        else if(tempType.compareToIgnoreCase("Lemma")==0)
                        {
                            bags = allLemmasToBags.get(instance);
                            if(bags==null){bags=new ArrayList<String>();allLemmasToBags.put(instance, bags);}
                        }
                        
                        bags.add(name);
                    }
                }
            }
            catch(Exception e)
            {
                throw new Exception("The file "+file.getAbsolutePath()+" is not in the correct format");
            }
        }
    }
    
        //This methods fills the attributes rulesFormIndex, rulesForm, rulesLemmaIndex, rulesLemma, rulesPOSIndex, rulesPOS, rulesNotAssociatedWithFormsLemmasAndPOSs
    private void loadCompiledSDFRules(File rootDirectoryCompiledSDFRules, String[] localPathsSDFRules) throws Exception
    {
        for(int j=0; j<localPathsSDFRules.length; j++)
        {
            //System.out.println(localPathsSDFRules[j]);
            //if(localPathsSDFRules[j].indexOf("E_FirstLevelChunking/POSs/Punctuations.xml")!=-1)
            //    i=i;

            String path = rootDirectoryCompiledSDFRules.getAbsolutePath()+"/"+localPathsSDFRules[j];
            if(path.lastIndexOf(".xml")!=path.length()-4)continue;
            path=path.substring(0, path.length()-4)+".sdf";
            
            if(new File(path).exists()==false)continue;

            InputStream is = new FileInputStream(path);
            InputStreamReader isr = new InputStreamReader(is, "UTF8");
            BufferedReader bf = new BufferedReader(isr);
            
                //Each line contains the SDFCode of one rule, but the first line, which contains the size of the XML file, we skip it.
            String SDFCode = bf.readLine();
            while((SDFCode=bf.readLine())!=null)indexSDFRuleOnFormLemmaPOS(SDFCode);
            
            is.close();
            isr.close();
            bf.close();
        }
    }
    
        //This method index an SDFRule with respect to the protected internal structures:
        //- rulesFormIndex
        //- rulesForm
        //- rulesLemmaIndex
        //- rulesLemma
        //- rulesPOSIndex
        //- rulesPOS
        //- rulesNotAssociatedWithFormsLemmasAndPOSs
        //The method is protected because we need it also in SDFCodesManager, which extends this class.
    protected void indexSDFRuleOnFormLemmaPOS(String SDFCode)throws Exception
    {
        String headAlternativesCode = SDFCode.substring(SDFCode.indexOf("$")+1, SDFCode.indexOf("$", SDFCode.indexOf("$")+1));

        ArrayList<String> keys = new ArrayList<String>();

            //Each <headAlternatives> can have multiple <head>(s) and each <head> can be defined on a different Form, Lemma, and POS
            //We collect them all. Of course, for each <head> we only keep the Form-Lemma-POS (in this order) and "OTHER" otherwise.
            //Therefore, for instance, if we have both Form and POS in some <head>, we only keep Form.
        while(headAlternativesCode.isEmpty()==false)
        {
            String headCode = headAlternativesCode.substring(headAlternativesCode.indexOf("#")+1, headAlternativesCode.indexOf("#", headAlternativesCode.indexOf("#")+1));
            headAlternativesCode = headAlternativesCode.substring(headAlternativesCode.indexOf("#", headAlternativesCode.indexOf("#")+1)+1, headAlternativesCode.length());

                //from convert2SDFCode.java:
                //£c --> Form
                //£d --> Lemma
                //£e --> POS
            String key = "OTHER";
            if(headCode.indexOf("£c")==0)
                key="form+++"+headCode.substring(headCode.indexOf("£c")+2, headCode.indexOf("£",headCode.indexOf("£c")+2)).toLowerCase();
            else if(headCode.indexOf("£d")==0)
                key="lemma+++"+headCode.substring(headCode.indexOf("£d")+2, headCode.indexOf("£",headCode.indexOf("£d")+2)).toLowerCase();
            else if(headCode.indexOf("£e")==0)
                key="POS+++"+headCode.substring(headCode.indexOf("£e")+2, headCode.indexOf("£",headCode.indexOf("£e")+2)).toLowerCase();

                //If there's OTHER we only have that as key: the SDFRule *needs* to be checked against every SDFNode, because each
                //of them can satisfy the <head> (of the <headAlternatives>) that corresponds to "OTHER"
            if(key.compareToIgnoreCase("OTHER")==0)
            {
                keys.clear();
                keys.add(key);
                break;
            }

            boolean t=false;
            for(int z=0;z<keys.size();z++){if(keys.get(z).compareToIgnoreCase(key)==0){t=true;break;}}
            if(t==false)keys.add(key);
        } 

        for(int z=0;z<keys.size();z++)
        {
            String key=keys.get(z);
            if(key.compareToIgnoreCase("OTHER")==0)rulesNotAssociatedWithFormsLemmasAndPOSs.add(SDFCode);
            else
            {
                ArrayList<String> keysOfrules = new ArrayList<String>();
                ArrayList<ArrayList<String>> rules = new ArrayList<ArrayList<String>>();

                if(key.indexOf("form+++")==0)
                {
                    keysOfrules = rulesFormIndex;
                    rules = rulesForm;
                    key = key.substring(7, key.length());
                }
                else if(key.indexOf("lemma+++")==0)
                {
                    keysOfrules = rulesLemmaIndex;
                    rules = rulesLemma;
                    key = key.substring(8, key.length());
                }
                else if(key.indexOf("POS+++")==0)
                {
                    keysOfrules = rulesPOSIndex;
                    rules = rulesPOS;
                    key = key.substring(6, key.length());
                }

                    //Add the line and the key to the vector of Form or Lemma
                addKeyAndLineToTheArray(keysOfrules, key, rules, SDFCode);
            }
        }
    }
    
        //This is an UTILITY used by the previous method only.
        //"array" is alphabethically ordered; this procedures looks for key via binary search. If it finds the key, it inserts line in the other array
        //at the same index. If it does not find it, it creates it at index and, again, it inserts there.
    private void addKeyAndLineToTheArray(ArrayList<String> array, String key, ArrayList<ArrayList<String>> rules, String line)throws Exception
    {
        int a = 0;
        int m = 0;
        int b = array.size()-1;

        while(b>=a)
        {
            m = (a+b)/2;
                
                //we found the key! we add the line in the other array at the same index and we return
            if(array.get(m).compareToIgnoreCase(key)==0){rules.get(m).add(line);return;}
            
                //If it is the latest element (but not the same), we have to create a new vector at this index (then we return).
                //This index can be either m or the one just after m.
            if((m==a)&&(m==b))
            {
                if(array.get(m).compareToIgnoreCase(key)<0)m++;
                
                    //We create a new pair at the index m
                array.add(m, key);
                rules.add(m, new ArrayList<String>());
                
                    //then we add and we return
                rules.get(m).add(line);
                return;
            }

                //binary search: we look for the place of key in the right half of the array
            if(array.get(m).compareToIgnoreCase(key)<0)a=m+1;
                //if m==a, we set b==a==m
            else if(m==a)b=a;else b=m-1;
        }
        
            //if we're here, the array is empty. We add one and we insert line there. Then we'll exit the procedure.
        array.add(key);
        rules.add(new ArrayList<String>());
        rules.get(0).add(line);
    }
}
