Haven Resource 1 src   UnivSprite.java /* Preprocessed source code */
package haven.res.lib.uspr;

import haven.*;
import java.util.*;
import haven.Skeleton.Pose;
import haven.Skeleton.PoseMod;
import haven.MorphedMesh.Morpher;

/* >spr: haven.res.lib.uspr.UnivSprite */
public class UnivSprite extends Sprite implements Gob.Overlay.CUpd, Skeleton.HasPose {
    public static final float ipollen = 0.3f;
    public final Skeleton skel;
    public final Pose pose;
    public PoseMod[] mods = new PoseMod[0];
    public MeshAnim.Anim[] manims = new MeshAnim.Anim[0];
    private Morpher.Factory mmorph;
    private final PoseMorph pmorph;
    private Pose oldpose;
    private float ipold;
    private boolean stat = true;
    private Rendered[] parts;
    private int fl;
    private boolean loading = false;

    public UnivSprite(Owner owner, Resource res, Message sdt) {
	super(owner, res);
	Skeleton.Res sr = res.layer(Skeleton.Res.class);
	if(sr != null) {
	    skel = sr.s;
	    pose = skel.new Pose(skel.bindpose);
	    pmorph = new PoseMorph(pose);
	} else {
	    skel = null;
	    pose = null;
	    pmorph = null;
	}
	fl = sdt.eom()?0xffff0000:decnum(sdt);
	update(true);
    }

    public Rendered animmesh(FastMesh mesh) {
	for(MeshAnim.Anim anim : manims) {
	    if(anim.desc().animp(mesh)) {
		Rendered ret = new MorphedMesh(mesh, mmorph);
		if(SkelSprite.bonedb)
		    ret = SkelSprite.morphed.apply(ret);
		return(ret);
	    }
	}
	Rendered ret;
	if(PoseMorph.boned(mesh)) {
	    String bnm = PoseMorph.boneidp(mesh);
	    if(bnm == null) {
		ret = new MorphedMesh(mesh, pmorph);
		if(SkelSprite.bonedb)
		    ret = SkelSprite.morphed.apply(ret);
	    } else {
		ret = pose.bonetrans2(skel.bones.get(bnm).idx).apply(mesh);
		if(SkelSprite.bonedb)
		    ret = SkelSprite.rigid.apply(ret);
	    }
	} else {
	    ret = mesh;
	    if(SkelSprite.bonedb)
		ret = SkelSprite.unboned.apply(ret);
	}
	return(ret);
    }

    public Rendered animwrap(Rendered r) {
	if(r instanceof FastMesh)
	    return(animmesh((FastMesh)r));
	if(r instanceof GLState.Wrapping) {
	    GLState.Wrapping wrap = (GLState.Wrapping)r;
	    Rendered nr = animwrap(wrap.r);
	    if(nr == wrap.r)
		return(wrap);
	    return(wrap.st().apply(nr));
	}
	return(r);
    }

    public Collection<Rendered> iparts(int mask) {
	Collection<Rendered> rl = new ArrayList<Rendered>();
	for(FastMesh.MeshRes mr : res.layers(FastMesh.MeshRes.class)) {
	    if((mr.mat != null) && ((mr.id < 0) || (((1 << mr.id) & mask) != 0)))
		rl.add(mr.mat.get().apply(animmesh(mr.m)));
	}
	return(rl);
    }

    private void chparts(int mask) {
	Collection<Rendered> rl = new ArrayList<Rendered>();
	for(Rendered r : iparts(mask))
	    rl.add(r);
	for(RenderLink.Res lr : res.layers(RenderLink.Res.class)) {
	    if((lr.id < 0) || (((1 << lr.id) & mask) != 0)) {
		Rendered r = lr.l.make();
		if(r instanceof GLState.Wrapping)
		    r = animwrap(r);
		rl.add(r);
	    }
	}
	this.parts = rl.toArray(new Rendered[0]);
    }

    private void rebuild() {
	if(skel != null) {
	    pose.reset();
	    for(PoseMod m : mods)
		m.apply(pose);
	    if(ipold > 0) {
		float f = ipold * ipold * (3 - (2 * ipold));
		pose.blend(oldpose, f);
	    }
	    pose.gbuild();
	}
    }

    private void chmanims(int mask) {
	Collection<MeshAnim.Anim> anims = new LinkedList<MeshAnim.Anim>();
	for(MeshAnim.Res ar : res.layers(MeshAnim.Res.class)) {
	    if((ar.id < 0) || (((1 << ar.id) & mask) != 0))
		anims.add(ar.make());
	}
	this.manims = anims.toArray(new MeshAnim.Anim[0]);
	this.mmorph = MorphedMesh.combine(this.manims);
    }

    private Map<Skeleton.ResPose, PoseMod> modids = new HashMap<Skeleton.ResPose, PoseMod>();
    private void chposes(int mask, boolean old) {
	if(!old) {
	    this.oldpose = skel.new Pose(pose);
	    this.ipold = 1.0f;
	}
	Collection<PoseMod> poses = new LinkedList<PoseMod>();
	stat = true;
	Skeleton.ModOwner mo = (owner instanceof Skeleton.ModOwner)?(Skeleton.ModOwner)owner:Skeleton.ModOwner.nil;
	Map<Skeleton.ResPose, PoseMod> newids = new HashMap<Skeleton.ResPose, PoseMod>();
	for(Skeleton.ResPose p : res.layers(Skeleton.ResPose.class)) {
	    if((p.id < 0) || ((mask & (1 << p.id)) != 0)) {
		Skeleton.PoseMod mod;
		if((mod = modids.get(p)) == null) {
		    mod = p.forskel(mo, skel, p.defmode);
		    if(old)
			mod.age();
		}
		if(p.id >= 0)
		    newids.put(p, mod);
		if(!mod.stat())
		    stat = false;
		poses.add(mod);
	    }
	}
	this.mods = poses.toArray(new PoseMod[0]);
	this.modids = newids;
	rebuild();
    }

    private void update(boolean old) {
	chmanims(fl);
	if(skel != null)
	    chposes(fl, old);
	chparts(fl);
	constant = new Gob.Static();
    }

    public void update() {
	try {
	    update(false);
	} catch(Loading l) {
	    loading = true;
	}
    }

    public void update(Message sdt) {
	fl = sdt.eom()?0xffff0000:decnum(sdt);
	update();
    }

    public boolean setup(RenderList rl) {
	for(Rendered p : parts)
	    rl.add(p, null);
	/* rl.add(pose.debug, null); */
	return(false);
    }

    public boolean tick(int idt) {
	float dt = idt / 1000.0f;
	if(loading) {
	    loading = false;
	    update();
	}
	if(!stat || (ipold > 0)) {
	    boolean done = true;
	    for(PoseMod m : mods) {
		m.tick(dt);
		done = done && m.done();
	    }
	    if(done)
		stat = true;
	    if(ipold > 0) {
		if((ipold -= (dt / ipollen)) < 0) {
		    ipold = 0;
		    oldpose = null;
		}
	    }
	    rebuild();
	}
	for(MeshAnim.Anim anim : manims)
	    anim.tick(dt);
	return(false);
    }

    private static final Object semistat;
    static {
	Object ss;
	try {
	    ss = Gob.SemiStatic.class;
	} catch(NoClassDefFoundError e) {
	    ss = CONSTANS;
	}
	semistat = ss;
    }
    private Object constant = new Gob.Static();
    public Object staticp() {
	if(!stat || (manims.length > 0) || (ipold > 0))
	    return(null);
	return((skel == null)?constant:semistat);
    }

    public Pose getpose() {
	return(pose);
    }
}
code �   haven.res.lib.uspr.UnivSprite ����   4�
 x � �	 q � �	 q �	 q �	 q � �
  �	 q � �
  �	 q � �
 � �	  �	 q � �
 � �	 � �
  �	 q � �
  �	 q �
 � ���  
 q �	 q �
 q �
  
 �	 q
 !		
	
 

 	 �	 +
 
			
 q	 3
 q
 3
 7 �	 q
 �!"#$%$&	 :'	 :(
)*	 :+
,	"-
 q./1	 G(	 G203"4 �	 q5
 6
 7	 q8@@  	 q9
 :
 ;<
 U �=	 W(
 W> �
 !?	 q@A	 ]CD	 _(	 _E
 _F
 GH
 I �
 qJ
 qK
 qL
 qMN
 qO
PQDz  
 R
 ST>���
 R	 qUVX	 qYZ\_ ipollen F ConstantValue skel Lhaven/Skeleton; pose Pose InnerClasses Lhaven/Skeleton$Pose; mods PoseMod [Lhaven/Skeleton$PoseMod; manims Anim [Lhaven/MeshAnim$Anim; mmorpha Morpherb Factory #Lhaven/MorphedMesh$Morpher$Factory; pmorph Lhaven/PoseMorph; oldpose ipold stat Z parts [Lhaven/Rendered; fl I loading modids Ljava/util/Map; 	Signature ResPose ALjava/util/Map<Lhaven/Skeleton$ResPose;Lhaven/Skeleton$PoseMod;>; semistat Ljava/lang/Object; constant <init>c Owner 6(Lhaven/Sprite$Owner;Lhaven/Resource;Lhaven/Message;)V Code LineNumberTable StackMapTableTcde � animmesh "(Lhaven/FastMesh;)Lhaven/Rendered; �/f animwrap "(Lhaven/Rendered;)Lhaven/Rendered; iparts (I)Ljava/util/Collection;gh +(I)Ljava/util/Collection<Lhaven/Rendered;>; chparts (I)V1 rebuild ()V chmanims= chposes (IZ)VAiD � update (Z)VN (Lhaven/Message;)V setup (Lhaven/RenderList;)Z tick (I)Z staticp ()Ljava/lang/Object;j getpose ()Lhaven/Skeleton$Pose; <clinit>X 
SourceFile UnivSprite.java �kl haven/Skeleton$PoseMod � �m haven/MeshAnim$Anim � � � � � � java/util/HashMap � � � �n haven/Gob$Static Static � � haven/Skeleton$Res Resdors  ~  haven/Skeleton$Posejtuv � �w � � haven/PoseMorph �x � �eyz{| � � � �}~� haven/MorphedMesh � � ���� ����������� �i�� haven/Skeleton$Bone Bone� �������� haven/FastMesh � � haven/GLState$Wrapping Wrapping�� � ��� java/util/ArrayList�� haven/FastMesh$MeshRes MeshRes��g��h�z� ���� ��������� � � haven/Rendered� haven/RenderLink$Res������ � �� ��x � | � ���� � java/util/LinkedList haven/MeshAnim$Res������ haven/Skeleton$ModOwner ModOwner�� haven/Skeleton$ResPose����� ��� �z � � � � � � � � haven/Loading � ���� ���z haven/res/lib/uspr/UnivSprite � � haven/Gob$SemiStatic 
SemiStatic java/lang/NoClassDefFoundError� � haven/Sprite� haven/Gob$Overlay$CUpd Overlay CUpd haven/Skeleton$HasPose HasPose haven/MorphedMesh$Morpher !haven/MorphedMesh$Morpher$Factory haven/Sprite$Owner haven/Resource haven/Message java/lang/String java/util/Collection java/util/Iterator java/util/Map java/lang/Object '(Lhaven/Sprite$Owner;Lhaven/Resource;)V haven/Skeleton haven/MeshAnim 	haven/Gob layer� Layer )(Ljava/lang/Class;)Lhaven/Resource$Layer; s getClass ()Ljava/lang/Class; bindpose ((Lhaven/Skeleton;Lhaven/Skeleton$Pose;)V (Lhaven/Skeleton$Pose;)V eom ()Z decnum (Lhaven/Message;)I desc ()Lhaven/MeshAnim; animp (Lhaven/FastMesh;)Z 6(Lhaven/FastMesh;Lhaven/MorphedMesh$Morpher$Factory;)V haven/SkelSprite bonedb morphed Lhaven/GLState; haven/GLState apply *(Lhaven/Rendered;)Lhaven/GLState$Wrapping; boned boneidp $(Lhaven/FastMesh;)Ljava/lang/String; bones get &(Ljava/lang/Object;)Ljava/lang/Object; idx 
bonetrans2 (I)Lhaven/Location; haven/Location rigid unboned r Lhaven/Rendered; st ()Lhaven/GLState; res Lhaven/Resource; layers )(Ljava/lang/Class;)Ljava/util/Collection; iterator ()Ljava/util/Iterator; hasNext next mat Lhaven/Material$Res; id haven/Material$Res ()Lhaven/Material; m Lhaven/FastMesh; haven/Material add (Ljava/lang/Object;)Z haven/RenderLink l Lhaven/RenderLink; make ()Lhaven/Rendered; toArray (([Ljava/lang/Object;)[Ljava/lang/Object; reset blend (Lhaven/Skeleton$Pose;F)V gbuild ()Lhaven/MeshAnim$Anim; combine I([Lhaven/MorphedMesh$Morpher$Factory;)Lhaven/MorphedMesh$Morpher$Factory; owner Lhaven/Sprite$Owner; nil Lhaven/Skeleton$ModOwner; defmode Lhaven/WrapMode; forskel� TrackMod T(Lhaven/Skeleton$ModOwner;Lhaven/Skeleton;Lhaven/WrapMode;)Lhaven/Skeleton$TrackMod; age put 8(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; haven/RenderList "(Lhaven/Rendered;Lhaven/GLState;)V (F)Z done CONSTANS haven/Gob$Overlay haven/Resource$Layer haven/Skeleton$TrackMod 
uspr.cjava ! q x  y z   { |  }    r  ~     � �    � �    � �    � �    � �    � �    � |    � �    � �    � �    � �    � �  �    �  � �    � �     � �  �  >     �*+,� *� � *� � *� *� *� Y� 	� 
*� Y� � ,� � :� 9*� � *� Y*� Y� W*� � � � *� Y*� � � � *� *� *� *-� � � -� � *� �    �   7 � |  � � � � �  L ��   � � � � �  � �   J               � + � 6  A  F  O  j  | ! � " � # � % � & � '  � �  �  a     �*� M,�>6� <,2:� +�  � $� !Y+*� "� #:� $� � %� &:�����+� '� Z+� (N-� !� !Y+*� � #M� $� ;� %,� &M� 0*� *� � )-� * � +� ,� -+� .M� $� � /,� &M� +M� $� � 0,� &M,�    �   9 �  Z� 5 � �� � � -  �� ,  � � �  � �  � �   V    *  + # , 1 - 7 . A / D * J 3 Q 4 V 5 Z 6 g 7 m 8 x : � ; � < � > � ? � @ � A � C  � �  �   �     :+� 1� *+� 1� 2�+� 3� $+� 3M*,� 4� 5N-,� 4� ,�,� 6-� &�+�    �    �  � ��  �   & 	   G  H  I  J  K % L - M / N 8 P  � �  �   �     g� 7Y� 8M*� 9:� ;� < N-� = � H-� > � ::� ?� 2� @� � @x~� ,� ?� A*� B� 2� C� D W���,�    �    �  � �� / �� �  �       T  U + V G W b X e Y �    �  � �  �       �� 7Y� 8M*� E� < N-� = � -� > � F:,� D W���*� 9G� ;� < N-� = � J-� > � G:� H� � Hx~� (� I� J :� 3� *� 5:,� D W���*,� F� K � L� M�    �   & �  � �� �  �� ' ��  �� �  �   2    ]  ^ ' _ 3 ` V a j b v c ~ d � e � g � h � i  � �  �   �     b*� � ]*� � N*� L+�=>� +2:*� � O����*� P�� #*� P*� PjQ*� PjfjD*� *� R#� S*� � T�    �    �  f� ( �   * 
   l  m  n " o + n 1 p : q N r Z t a v  � �  �   �     k� UY� VM*� 9W� ;� < N-� = � 1-� > � W:� X� � Xx~� ,� Y� D W���*,� � K � Z� **� � [� "�    �    �  � �� ' �� �  �   "    y  z + { ? | K } N ~ _  j �  � �  �  �  	  �  *� Y*� Y� W*� � � R*� P� UY� VN*� *� \� ]� *� \� ]� � ^:� Y� 	:*� 9_� ;� < :� = � � > � _:� `� � `x~� \*� 
� * � Y:� *� � a� b:� � c� `� � d W� e� *� -� D W��}*-� � K � f� *� 
*� g�    �   - 
!�   �B ��  � � �� ) �� . �� �  �   ^    �  �  � ! � ) � . � G � P � v � � � � � � � � � � � � � � � � � � � � � � � � � � � �  � �  �   a     ,**� � h*� � **� � i**� � j*� Y� � �    �     �       �  �  �  �   � + �  � �  �   L     *� � 	L*� �      k  �    H � �       �  �  � 	 �  �  � �  �   V     *+� � � +� � *� l�    �    M ��   � �  � �       �  �  �  � �  �   ]     &*� MM,�>6� ,2:+� m�����    �    �  L�  �       �  �  � $ �  � �  �  j     ��nnE*� � *� *� l*� � *� P�� s>*� :�66� )2:$� oW� � p� � >����� *� *� P��  *Y� P$rnfZ� P�� *� P*� R*� g*� N-�66� -2:$� sW�����    �   > � �   � f  � $ �@�   �  %� �  Z�  �   V    �  �  �  �  � & � ( � D � K � ] � c � g � l � u � � � � � � � � � � � � � � �  � �  �   [     ,*� � *� �� *� P�� �*� � 
*� � � t�    �   	 B � �       �  �  �  � �  �        *� �    �       �  � �  �   V     uK� L� wK*� t�      v  �    F ��  � �       �  �  �  �  �  �  �   � �   �   � �   � �  � � � ! �	 � � �	 _ � � 	 � x �	  � � 	  � � 	 + � 	 3  : 1  	 G0 � 	 W � � 	 ] �B	 u �W 	[ �] 	 y[^	 z �`	p �q), � 	� �� codeentry %   spr haven.res.lib.uspr.UnivSprite   