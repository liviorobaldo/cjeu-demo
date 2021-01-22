package SDFTagger.KBInterface.SDFCodesInterface;
import SDFTagger.KBInterface.XMLFilesInterface.XML2SDFCodesCompiler.convert2SDFCode;
import SDFTagger.KBInterface.XMLFilesInterface.XMLFilesManager;
import java.io.StringReader;
import java.util.*;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;

/**
LEGGI QUESTI COMMENTI!!!!!

Solo che XMLFilesManager prende le regole da files XML. Qui invece le componiamo al volo con l Xpath, utilizzando la classe SDFCodeFactory.

Tra l altro, non mi piace il nome XMLFilesManager... cambialo (ANCHE NEL MANUALE!!!)

Invece SDFCodeFactory produce le regole (una per una, quindi bisogna ricreare l oggetto Java ogni volta...
però forse si può pensare ad un metodo 'reset'? Non so, ci devo pensare... comodo per la gestione della memoria ma fa un po schifo..)
/**/

    //We extend XMLFilesManager because we need:
    //  - its protected attributes:
    //      protected Hashtable<String, ArrayList<String>> allFormsToBags = new Hashtable<String,ArrayList<String>>();
    //      protected Hashtable<String, ArrayList<String>> allLemmasToBags = new Hashtable<String,ArrayList<String>>();
    //      protected ArrayList<String> rulesFormIndex = new ArrayList<String>();
    //      protected ArrayList<ArrayList<String>> rulesForm = new ArrayList<ArrayList<String>>();
    //      protected ArrayList<String> rulesLemmaIndex = new ArrayList<String>();
    //      protected ArrayList<ArrayList<String>> rulesLemma = new ArrayList<ArrayList<String>>();
    //      protected ArrayList<String> rulesPOSIndex = new ArrayList<String>();
    //      protected ArrayList<ArrayList<String>> rulesPOS = new ArrayList<ArrayList<String>>();
    //      protected ArrayList<String> rulesNotAssociatedWithFormsLemmasAndPOSs = new ArrayList<String>();
    //  - The two methods of the KBInterface (implemented by XMLFilesManager: we need the *IMPLEMENTATION* of these methods from XMLFilesManager):
    //      public void fillBagsOfSDFNode(String Form, String Lemma, ArrayList<String> bagsOnForm, ArrayList<String> bagsOnLemma)throws Exception;
    //      public ArrayList<String> retrieveSDFCodes(SDFHead SDFHead)throws Exception;
    //
    //What we don't need are the methods from XMLFilesManager that populates the protected attributes listed above. Here they are different and we
    //will rewrite them. But, in fact, in XMLFilesManager these methods are *PRIVATE* utilities that cannot be accessed from here, so who cares.
public class SDFCodesManager extends XMLFilesManager
{
    public SDFCodesManager(ArrayList<String> SDFCodes)throws Exception
    {
            //This is a trick... the constructor of XMLFilesManager does not do anything if the local paths are empty arrays of String(s), i.e., String[0]
            //I know it's idiotic, but I prefer this than defining a fake constructor in XMLFilesManager...
        super(null, new String[0], null, null, new String[0]);
        
            //We load the SDFCodes in input. setSDFCodes first makes the IDs unique and then 
            //loads the SDFRule(s) in the attributes inherited from XMLFilesManager.
        setSDFCodes(SDFCodes);
    }
    
        //This method adds an ArrayList of SDFRule(s) encoded in SDFCode(s) to the protected attributes above.
        //But first, it clears that protected attributes.
        //Therefore, we basically recreate an SDFCodesManager whenever this method is called. This SDFCodesManager will refer ONLY to the ArrayList<String> 
        //as parameter. Same for the bags in the next method. This choice has been taken because it would too complex to manage the ids of SDFRule(s) which
        //are already in the protected attributes and SDFRule(s) which are not. By imposing this, it is simpler: first we assign unique ids to the SDFCodes
        //in input, then we load the SDFCodes in the protected attributes.
    private void setSDFCodes(ArrayList<String> SDFCodes)throws Exception
    {
        rulesFormIndex.clear();
        rulesForm.clear();
        rulesLemmaIndex.clear();
        rulesLemma.clear();
        rulesPOSIndex.clear();
        rulesPOS.clear();
        rulesNotAssociatedWithFormsLemmasAndPOSs.clear();
        
            //First thing to do: reset the ids of all rules, in order to make them unique (no same id for two or more rules).
            //Then we put the modified SDFCode back in the input array, and we index it in the protected attributes in the beginning
            //with the indexSDFRuleOnFormLemmaPOS method (inherited from XMLFilesManager).
        for(int i=0;i<SDFCodes.size();i++)
        {
            String newSDFCode = SDFCodes.remove(i);
            newSDFCode = (i+1)+newSDFCode.substring(newSDFCode.indexOf("£"), newSDFCode.length());
            SDFCodes.add(i, newSDFCode);
            indexSDFRuleOnFormLemmaPOS(newSDFCode);
        }
    }
    
        //Same of above, but we the bags. We clear the old ones and we copy the content of the one as parameter.
    private void setSDFBags(Hashtable<String, ArrayList<String>> allFormsToBags, Hashtable<String, ArrayList<String>> allLemmasToBags)
    {
        this.allFormsToBags.clear();
        this.allLemmasToBags.clear();
        for(String Form:allFormsToBags.keySet())this.allFormsToBags.put(Form, allFormsToBags.get(Form));
        for(String Lemma:allLemmasToBags.keySet())this.allLemmasToBags.put(Lemma, allLemmasToBags.get(Lemma));
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    public static void main1(String args[]) throws Exception 
    {
        try
        {
            SAXBuilder sb= new SAXBuilder();

            String docString = 
                "<SDFRule id=\"1\" priority=\"40\">\n" +
                "  <tag>TOPtag</tag>\n" +
                "  <headAlternatives>\n" +
                "    <head>\n" +
                "      <Lemma>TOP</Lemma>\n" +
                "    </head>\n" +
                "  </headAlternatives>\n" +
                "  <prevAlternatives>\n" +
                "    <prev maxDistance=\"1\">\n" +
                "      <headAlternatives>\n" +
                "        <head>\n" +
                "          <Form>Prev</Form>\n" +
                "        </head>\n" +
                "      </headAlternatives>\n" +
                "       <prevAlternatives>\n" +
                "           <prev maxDistance=\"1\">\n" +
                "               <headAlternatives>\n" +
                "                   <head>\n" +
                "                       <Form>PrevPrev</Form>\n" +
                "                   </head>\n" +
                "               </headAlternatives>\n" +
                "           </prev>\n" +
                "       </prevAlternatives>\n" +
                "    </prev>\n" +
                "  </prevAlternatives>\n" +
                "</SDFRule>";
            
            Document doc = sb.build(new StringReader(docString));
            convert2SDFCode convert2SDFCode = new convert2SDFCode(doc.getRootElement());
            System.out.println(convert2SDFCode.getSDFCode());
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }
    }
    
    public static void main(String args[]) throws Exception 
    {
        try
        {
            ArrayList<String> codes = new ArrayList<String>();
            
            Hashtable<String, String> pairsSlotValue = new Hashtable<String, String>();
            pairsSlotValue.put("Form", "TOP");
            SDFCodeFactory SDFCodeFactory = new SDFCodeFactory(40, pairsSlotValue, new Hashtable<String, ArrayList<String>>());
            SDFCodeFactory.addSDFTag("SDFRule", "TOPtag");
                        
            pairsSlotValue.clear();
            pairsSlotValue.put("POS", "NOUN");
            pairsSlotValue.put("Form", "Prev");
            SDFCodeFactory.addPrev("SDFRule", 1, pairsSlotValue, new Hashtable<String, ArrayList<String>>());
            
            /**/
            pairsSlotValue.clear();
            pairsSlotValue.put("Lemma", "provaPrev2");
            pairsSlotValue.put("Form", "PrevPrev");
            SDFCodeFactory.addPrev("SDFRule/Prev[1]", 1, pairsSlotValue, new Hashtable<String, ArrayList<String>>());
            
            /**/
            SDFCodeFactory.addSDFTag("SDFRule/Prev[1]/Prev[1]", "Eccoci");
            /**/
            
            codes.add(SDFCodeFactory.getSDFCode());
            
            pairsSlotValue.clear();
            pairsSlotValue.put("Lemma", "secondo");
            pairsSlotValue.put("Form", "me");
            SDFCodeFactory = new SDFCodeFactory(13, pairsSlotValue, new Hashtable<String, ArrayList<String>>());
            SDFCodeFactory.addSDFTag("SDFRule", "SempreTOP");
                        
            pairsSlotValue.clear();
            pairsSlotValue.put("Form", "Next");
            pairsSlotValue.put("POS", "UFFA");
            SDFCodeFactory.addNext("SDFRule", 1, pairsSlotValue, new Hashtable<String, ArrayList<String>>());
            
            pairsSlotValue.clear();
            pairsSlotValue.put("Form", "NextStart");
            SDFCodeFactory.addNextStar("SDFRule/Next[1]", 1, pairsSlotValue, new Hashtable<String, ArrayList<String>>());
            SDFCodeFactory.addSDFTag("SDFRule/Next[1]/NextStar[1]", "Finoaqui");
            
            codes.add(SDFCodeFactory.getSDFCode());
            
            //System.out.println(SDFCode);
            //System.out.println("\n\nXML:");
            //System.out.println(SDFCodeFactory.getXMLString());
            
            //ora prova a costruire una SDFRule e poi chiama un buildSDFRuleXML()
            
            SDFCodesManager SDFCodesManager = new SDFCodesManager(codes);
            
            SDFCodesManager = SDFCodesManager;
            
        }
        catch(Exception e)
        {
            System.out.println("Eccezione! Message: \""+e.getMessage()+"\"");
        }
    }
}