SpawnDecoration
================
A simple spigot plugin to add cool animated decorations to your minecraft spawn.


**Demo video**: https://youtu.be/7_gPurFwPRg  
**Support Discord**: https://discord.gg/PUnTJHThxJ

**How to use it**:  
**/spawndecoration** -> getInfo and help of the command  
**/spawndecoration reload** -> reload the plugin, load and respawn the models  
**/spawndecoration record <RecordName>** -> start a new record,
left click to stop the record is automaticly save in the **recordlocation**
folder  
**DecorationFile** go to folder decorations and create new .yml file inside you can create
decorations as much as you want example of configuration bellow  
**Modelisation** if you want the same result as mine, use segmented part,
put in front of the name of you bone **seg_** and put bone in bone, the first bone
should be generaly the head so put **h_** in the front of the head bone name
you can animate the model if you created a idle animation

**Notes:**  
If you find bug mp me
Obviously this plugin uses modelengine as a dependency  
You can create a subfolder inside the **recordlocation** and **decorations** folder
as much as you want to organise all your files

Example of decoration configuration:

```yaml
# Name of the decoration (put anything you want doesn't really matter)
light_thing_red_orange_1:
  # ModelEngine model id of the decoration
  model: "lightspeed_orange_red"
  # Name of the record you recorded with the command (record name file)
  record: "lightspeed_1"

light_thing_red_orange_2:
  model: "lightspeed_orange_red"
  record: "lightspeed_2"

dragon:
  model: "jap_red_dragon"
  record: "jap_red_dragon"
```