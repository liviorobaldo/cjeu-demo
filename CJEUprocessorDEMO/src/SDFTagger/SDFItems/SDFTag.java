package SDFTagger.SDFItems;

//The SDFTagger associate tags with SDFHead(s). 
//- An SDFHead may have multiple tags.
//- Each tag has a priority, which is equal to the priority of the SDFRule that assigned it.
//- Each tag has an idSDFRule, which is equal to the id of the SDFRule that assigned it.
//- Each tag has an idInstance, in that it is possible that the *same* SDFRule assign the *same* tag to the *same* SDFHead. To distinguish them, each tag is also associated 
//  to a unique identifier, which is a incremental counter of SDFRuleManager (each time it is assigned, it is incremented +1).

public class SDFTag 
{
    public String tag = null;
    public SDFHead taggedHead = null;
    public long priority;
    public long idSDFRule;
    public long idInstance;
    
    public SDFTag(String tag, SDFHead taggedHead, long priority, long idSDFRule, long idInstance)
    {
        this.tag = tag; 
        this.taggedHead = taggedHead;
        this.priority = priority;
        this.idSDFRule = idSDFRule;
        this.idInstance = idInstance;
    }
}
