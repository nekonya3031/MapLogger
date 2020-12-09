package mapLogger;

import arc.*;
import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.g2d.TextureAtlas;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.CommandHandler;
import arc.util.io.CounterInputStream;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.ContentLoader;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.io.MapIO;
import mindustry.io.SaveIO;
import mindustry.io.SaveVersion;
import mindustry.maps.Map;
import mindustry.mod.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.OreBlock;
import mindustry.world.blocks.environment.StaticWall;


import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.zip.InflaterInputStream;

import static mindustry.Vars.state;
import static mindustry.Vars.world;

public class Primary extends Plugin{
    Color co=new Color();
    Graphics2D currentGraphics;
    BufferedImage currentImage;
    ContentLoader content;
    Seq<BufferedImage> gif=new Seq<>();
    @Override
    public void init(){
        try{
            BufferedImage image = ImageIO.read(new File("block_colors.png"));

            for(Block block : Vars.content.blocks()){
                block.mapColor.argb8888(image.getRGB(block.id, 0));
                if(block instanceof OreBlock){
                    block.mapColor.set(((OreBlock)block).itemDrop.color);
                }
            }
        }catch(Exception e){
            throw new RuntimeException(e);
        }
        content=Vars.content;

        Events.on(Trigger.update.getClass(), event -> {
            if(state.isGame()){
            try {
                gif.add(readMap());
            } catch (IOException e) {
                e.printStackTrace();
            }}
        });
        Events.on(GameOverEvent.class,event->{
            try {
                createGif(event.winner.name+new Date().toString(),gif);
            } catch (IOException e) {
                e.printStackTrace();
            }
            gif=new Seq<>();
        });
    }

    public BufferedImage readMap() throws IOException {
        int y=world.tiles.height;
        int x=world.tiles.width;
            BufferedImage image=new BufferedImage(x,y,BufferedImage.TYPE_INT_ARGB);
            java.awt.Color c=new java.awt.Color(0,255,0);

            int a=0,b=0;
            while(a<x){
                b=0;
                while(b<y){
                    int ccc=conv(MapIO.colorFor(content.block(world.tiles.get(a,b).blockID()), Vars.content.block(world.tiles.get(a,b).floorID()), Vars.content.block(world.tiles.get(a,b).overlayID()),world.tiles.get(a,b).team()));
                    image.setRGB(a,b,ccc);
                    b++;
                }
                a++;
            }
            int ty=y-5;
            int tx=x+5;
            for(Player p:Groups.player){
                int ux=p.tileX();
                int uy=p.tileY();
                image.setRGB(ux,uy,p.team().color.rgba8888());
            }
            return image;
    }

    int conv(int rgba){
        return co.set(rgba).argb8888();
    }

    public void createGif(String name,Seq<BufferedImage> img) throws IOException {
        BufferedImage firstImage = img.first();
        img.remove(1);
        Fi ff=new Fi("config/replays/"+name.replace(' ','_').replace(':','_')+".gif");
        ff.write();ff.write().close();
        File f=new File(ff.absolutePath());
        // create a new BufferedOutputStream with the last argument
        ImageOutputStream output =
                new FileImageOutputStream(f);
        // create a gif sequence with the type of the first image, 1 second
        // between frames, which loops continuously
        GifSequenceWriter writer =
                new GifSequenceWriter(output, firstImage.getType(), 30, true);

        // write out the first image to our sequence...
        writer.writeToSequence(firstImage);
        for(BufferedImage nextImage:img) {
            writer.writeToSequence(nextImage);
        }

        writer.close();
        output.close();
    }


}
