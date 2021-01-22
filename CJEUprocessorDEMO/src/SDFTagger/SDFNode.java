package SDFTagger;

import SDFTagger.SDFItems.SDFHead;
import java.util.*;

public class SDFNode
{
        //All nodes of a set of SDFDependencyTrees are bi-directionally chained.
    
        //SDFNodes have an SDFHead and an index, which marks their position in the chain.
        //The index is needed to quickly check the position of an SDFNode wrt to another one.
    protected SDFHead SDFHead = null;
    protected int index = -1;
    
        //Attributes needed to bi-directionally chain all SDFNodes. All nodes of a *SET* of SDFDependencyTrees are bi-directionally chained.
    protected SDFNode nextSDFNode = null;
    protected SDFNode prevSDFNode = null;
    
        //The SDFHead enclosed within an SDFNode is associated with some bags, either on its Lemma or on its Form. Bags are taken from the KBInterface.
        //Some constraints in the rules can require the enclosed SDFHead(s) to belong to some bag. 
        //Moreover, the SDFNode contains a boolean "endOfSentence" which is true if the SDFHead is the last SDFHead of the SDFDependencyTree, false otherwise
    protected ArrayList<String> bagsOnForm = new ArrayList<String>();
    protected ArrayList<String> bagsOnLemma = new ArrayList<String>();
    protected boolean endOfSentence = false;
    
        //All attributes are protected, they cannot be manipulated outside the package (but it is useful to 
        //have direct access from the SDFRuleManager class).
    public SDFNode(SDFHead SDFHead, int index, ArrayList<String> bagsOnForms, ArrayList<String> bagsOnLemmas, boolean endOfSentence)
    {
        this.index=index;
        this.SDFHead=SDFHead;
        this.endOfSentence=endOfSentence;
        if(bagsOnForms!=null)
            for(int i=0;i<bagsOnForms.size();i++)
                this.bagsOnForm.add(bagsOnForms.get(i));
        if(bagsOnLemmas!=null)
            for(int i=0;i<bagsOnLemmas.size();i++)
                this.bagsOnLemma.add(bagsOnLemmas.get(i));
    }
}