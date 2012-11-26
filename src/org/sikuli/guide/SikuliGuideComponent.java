/**
 *
 */
package org.sikuli.guide;

import java.awt.AlphaComposite;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JComponent;

import org.sikuli.guide.util.ComponentMover;
import org.sikuli.script.Debug;
import org.sikuli.script.Region;



public class SikuliGuideComponent extends JComponent
implements Cloneable{

   public enum Layout{
      TOP,
      BOTTOM,
      LEFT,
      RIGHT,
      FOLLOWERS,
      INSIDE,
      OVER,
      ORIGIN,
      CENTER
   };

   static boolean DEBUG_BOUNDS = false;

   public String toString(){
      return "" + getClass() + " " + "[actualBounds=" + getActualBounds() + "]";
   }

   public Object clone() {
      SikuliGuideComponent clone;
      try {
         clone =  (SikuliGuideComponent) super.clone();

         // do not clone references to other components
         clone.followers = new ArrayList<SikuliGuideComponent>();
         clone.removeFromLeader();
         clone.actualBounds = new Rectangle(actualBounds);
         clone.autolayout = null;
         //clone.connectors = new ArrayList<Connector>();
         return clone;
      }
      catch (CloneNotSupportedException e) {
         throw new InternalError(e.toString());
      }
   }

   ShadowRenderer shadowRenderer;

   int shadowSize = 0;
   int shadowOffset = 2;

   public void setShadowDefault(){
      setShadow(10,2);
   }

   public void setShadow(int shadowSize, int shadowOffset){
      this.shadowSize = shadowSize;
      this.shadowOffset = shadowOffset;

      shadowRenderer = new ShadowRenderer(this, shadowSize);
      super.setSize(getActualWidth()+2*shadowSize, getActualHeight()+2*shadowSize);
      Point p = getActualLocation();
      p.x = p.x - shadowSize + shadowOffset;
      p.y = p.y - shadowSize + shadowOffset;
      super.setLocation(p.x,p.y);
   }


   class AnimationSequence {
      Queue<NewAnimator> queue = new LinkedBlockingQueue<NewAnimator>();

      private void startNextAnimation(){
         if (queue.peek() != null){

            NewAnimator anim = queue.remove();
            anim.start();
            anim.setListener(new AnimationListener(){

               @Override
               public void animationCompleted() {
                  startNextAnimation();
               }
            });

         }
      }

      public void add(NewAnimator animator){
         queue.add(animator);
      }

      public void start(){
         startNextAnimation();
      }
   }

   AnimationSequence animationSequence = new AnimationSequence();

   AnimationFactory getAnimationFactor(){
      return new AnimationFactory();
   }

   public void addAnimation(NewAnimator anim){
      animationSequence.add(anim);
   }

   public void addMoveAnimation(Point source, Point destination){
      animationSequence.add(AnimationFactory.createMoveAnimation(this, source, destination));
   }

   public void addResizeAnimation(Dimension currentSize, Dimension targetSize){
      animationSequence.add(AnimationFactory.createResizeAnimation(this, currentSize, targetSize));
   }

   public void addCircleAnimation(Point origin, float radius){
      animationSequence.add(AnimationFactory.createCircleAnimation(this, origin, radius));
   }

   public void addFadeinAnimation(){
      if (opacity < 1f)
         animationSequence.add(AnimationFactory.createOpacityAnimation(this,opacity,1f));
   }

   public void addFadeoutAnimation(){
      if (opacity > 0f)
         animationSequence.add(AnimationFactory.createOpacityAnimation(this,opacity,0f));
   }

   public void addSlideAnimation(Point destination, Layout side){
      Point p0 = new Point(destination);
      Point p1 = new Point(destination);

      if (side == Layout.RIGHT){
         p0.x += 20;
      } else if (side == Layout.BOTTOM){
         p0.y += 20;
      } else if (side == Layout.TOP) {
         p0.y -= 20;
      } else if (side == Layout.LEFT) {
         p0.x -= 20;
      }

      setActualLocation(p0);
      addMoveAnimation(p0, p1);
   }

   public void startAnimation(){
      animationSequence.start();
   }

   public void stopAnimation() {
      if (emphasis_anim != null){
         emphasis_anim.stop();
      }
      if (entrance_anim != null){
         entrance_anim.stop();
      }
   }

   public SikuliGuideAnimator createSlidingAnimator(int offset_x, int offset_y){
      Point dest = getActualLocation();
      Point src = new Point(dest.x + offset_x, dest.y + offset_y);
      return new MoveAnimator(this, src, dest);
   }

   public SikuliGuideAnimator createMoveAnimator(int dest_x, int dest_y){
      Point src = getActualLocation();
      Point dest = new Point(dest_x, dest_y);
      return new MoveAnimator(this, src, dest);
   }

//   public SikuliGuideAnimator createCirclingAnimator(int radius) {
//      return new CircleAnimator(this, radius);
//      return nu
//   }








   boolean animationRunning = false;

   float opacity = 1.0f;

   public void paintPlain(Graphics g){
      super.paint(g);
   }

  @Override
   public void paint(Graphics g){

         // render the component in an offscreen buffer with shadow
         BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
         Graphics2D g2 = image.createGraphics();

         if (shadowRenderer != null){
            shadowRenderer.paintComponent(g2);
            g2.translate((shadowSize-shadowOffset),(shadowSize-shadowOffset));
         }

         super.paint(g2);

         Graphics2D g2d = (Graphics2D) g;
         ((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,opacity));
         g2d.drawImage(image,0,0,null,null);
   }

   public void resizeTo(Dimension targetSize){
      //ResizeAnimator anim = new ResizeAnimator(this, getActualSize(),targetSize);
      //anim.start();
   }

   public void moveTo(Point targetLocation){
      NewAnimator anim = AnimationFactory.createCenteredMoveAnimation(this, getActualLocation(), targetLocation);
      anim.start();
   }

   public void moveTo(Point targetLocation, AnimationListener listener){
      NewAnimator anim = AnimationFactory.createCenteredMoveAnimation(this, getActualLocation(), targetLocation);
      anim.setListener(listener);
      anim.start();
   }

   public void popupOLD(){
//      PopupAnimator anim = new PopupAnimator();
//      anim.start();
   }

   public void popin(){
      Dimension targetSize = new Dimension(getActualSize());
      targetSize.width /= 1.2;
      targetSize.height /= 1.2;
      NewAnimator anim = AnimationFactory.createCenteredResizeToAnimation(this, targetSize);
      anim.start();
   }

   public void popout(){
      setShadowDefault();

      Dimension targetSize = new Dimension(getActualSize());
      targetSize.width *= 1.2;
      targetSize.height *= 1.2;
      NewAnimator anim = AnimationFactory.createCenteredResizeToAnimation(this, targetSize);
      anim.start();
   }

   public void setOpacity(float opacity){

      if (opacity > 0){
         setVisible(true);
      }else{
         setVisible(false);
      }

      this.opacity = opacity;
      for (SikuliGuideComponent sklComp : getFollowers()){
         sklComp.setOpacity(opacity);
      }
//      if (shadowRenderer != null){
//         shadowRenderer.createShadowImage();
//      }


      Rectangle r = getBounds();
      if (getTopLevelAncestor() != null) {
       getTopLevelAncestor().repaint();
     }
   }

//   public void changeOpacityTo(float targetOpacity){
//      OpacityAnimator anim = new OpacityAnimator(this, opacity,targetOpacity);
//      anim.start();
//   }


   SikuliGuideAnimator entrance_anim;
   SikuliGuideAnimator emphasis_anim;

   public void setEntranceAnimation(SikuliGuideAnimator anim){
      if (entrance_anim != null)
         entrance_anim.stop();
      else
         entrance_anim = anim;
   }

   public void setEmphasisAnimation(SikuliGuideAnimator anim){
      if (emphasis_anim != null)
         emphasis_anim.stop();

      if (entrance_anim != null)
         entrance_anim.stop();

      emphasis_anim = anim;
   }

   public final static int TOP = 0;
   public final static int LEFT = 1;
   public final static int RIGHT = 2;
   public final static int BOTTOM = 3;
   public final static int INSIDE = 4;
   public final static int CENTER = 5;
   public final static int OVER = 6;
   public final static int ORIGIN = 7;
   public final static int FOLLOWERS = 8;

   private boolean autoLayoutEnabled = false;
   private boolean autoResizeEnabled = false;
   private boolean autoMoveEnabled = false;
   private boolean autoVisibilityEnabled = false;

   public SikuliGuideComponent(){
      super();
      setMovable(false);
      setActualLocation(0,0);
      setActualSize(new Dimension(0,0));
   }

   // this allows the component to be dragged to another location on the screen
   ComponentMover cm = new ComponentMover();
   public void setMovable(boolean movable){
      if (movable){
         cm.registerComponent(this);
      }else{
         cm.deregisterComponent(this);
      }
   }

   class AutoLayout implements ComponentListener {
      private SikuliGuideComponent targetComponent;
      AutoLayout(SikuliGuideComponent targetComponent){
         this.setTargetComponent(targetComponent);
         //targetComponent.addComponentListener(this);
      }
      public void setTargetComponent(SikuliGuideComponent targetComponent) {
         this.targetComponent = targetComponent;
      }
      public SikuliGuideComponent getTargetComponent() {
         return targetComponent;
      }

      void update() {

         //Debug.info("Update caused by leader:" + this);

         // TODO calculate necesary region to udpate
//         if (getParent()!=null){
//
//            if (getParent().getParent()!=null){
//               getParent().getParent().repaint();
//            }else{
//               getParent().repaint();
//            }
//         }

      }

      void stop(){
        // targetComponent.removeComponentListener(this);
      }

      @Override
      public void componentHidden(ComponentEvent e) {

//         if (isAutoVisibilityEnabled()){
//            setVisible(false);
//            update();
//         }
      }

      @Override
      public void componentMoved(ComponentEvent e) {
//         if (isAutoMoveEnabled())
//            update();
      }

      @Override
      public void componentResized(ComponentEvent e) {
//         if (isAutoResizeEnabled())
//            update();
      }

      @Override
      public void componentShown(ComponentEvent e) {
//         if (isAutoVisibilityEnabled()){
//            setVisible(true);
//            update();
//         }
      }

   }

   class AutoLayoutBySide extends AutoLayout{
      Layout side;

      AutoLayoutBySide(SikuliGuideComponent targetComponent, Layout side){
         super(targetComponent);
         this.side = side;
      }

      @Override
      void update(){

         if (side == Layout.FOLLOWERS){

            // set to the total bounds of the other followers

            // first set its bounds to be equal to the targets, so that
            // its current bounds won't have effect on the calculation
            // of the total bounds
            setBounds(getTargetComponent().getBounds());

            // then this call will gives us the total bounds of the
            // rest of the followers
            Rectangle totalBounds = getTargetComponent().getFollowerBounds();

            totalBounds.grow(5,5);

            setBounds(totalBounds);


         }else{

            //Debug.info("Target actual bounds: " + getTargetComponent().getActualBounds());
            Region region = new Region(getTargetComponent().getActualBounds());
            setLocationRelativeToRegion(region, side);
         }
         super.update();
      }
   }

   class AutoLayoutByMovement extends AutoLayout {
      // previous known location of the target this component follows
      int x;
      int y;

      Point targetLocation;

      AutoLayoutByMovement(SikuliGuideComponent targetComponent){
         super(targetComponent);
         targetLocation = new Point(targetComponent.getActualLocation());
         this.x = targetComponent.getX();
         this.y = targetComponent.getY();
      }


      @Override
      public void update() {

         //Debug.info("auto moved by leader");

         Point newTargetLocation = getTargetComponent().getActualLocation();
         int dx = newTargetLocation.x - targetLocation.x;
         int dy = newTargetLocation.y - targetLocation.y;
         targetLocation = newTargetLocation;
         Point actualLocation = getActualLocation();
         actualLocation.x += dx;
         actualLocation.y += dy;

         setActualLocation(actualLocation.x,actualLocation.y);
      }
   }

   class AutoLayoutByOffset extends AutoLayout {
      int offsetx;
      int offsety;

      AutoLayoutByOffset(SikuliGuideComponent targetComponent, int offsetx, int offsety){
         super(targetComponent);
         this.offsetx = offsetx;
         this.offsety = offsety;
      }

      @Override
      void update(){
         setOffset(offsetx, offsety);
         Region region = new Region(leader.getBounds());
         setLocationRelativeToRegion(region, Layout.ORIGIN);
         super.update();
      }
   }

   class AutoLayoutByRatio extends AutoLayout{
      float x, y;

      AutoLayoutByRatio(SikuliGuideComponent targetComponent, float x, float y){
         super(targetComponent);
         this.x = x;
         this.y = y;
      }

      @Override
      void update(){
         Region region = new Region(getTargetComponent().getBounds());
         setHorizontalAlignmentWithRegion(region, x);
         setVerticalAlignmentWithRegion(region, y);
         super.update();
      }
   }

   AutoLayout autolayout = null;

   public void setLocationRelativeToComponent(SikuliGuideComponent comp, Layout side) {
      if (autolayout != null){
         autolayout.stop();
      }

      comp.addFollower(this);

      autolayout = new AutoLayoutBySide(comp, side);
      autolayout.update();
   }

   public void setLocationRelativeToComponent(SikuliGuideComponent comp, int offsetx, int offsety) {
      if (autolayout != null){
         autolayout.stop();
      }

      comp.addFollower(this);

      autolayout = new AutoLayoutByOffset(comp, offsetx, offsety);
      autolayout.update();
   }

   public void setLocationRelativeToComponent(SikuliGuideComponent comp, float relativeX, float relativeY) {
      if (autolayout != null){
         autolayout.stop();
      }

      autolayout = new AutoLayoutByRatio(comp, relativeX, relativeY);
      autolayout.update();
   }

   public void setLocationRelativeToComponent(SikuliGuideComponent leader){
      if (autolayout != null){
         autolayout.stop();
      }

      leader.addFollower(this);

      autolayout = new AutoLayoutByMovement(leader);
      autolayout.update();
   }


   public void setLocationRelativeToPoint(Point point, Layout side){
      Rectangle bounds = getActualBounds();
      // TODO implement other positioning parameters
      if (side == Layout.CENTER){
         setActualLocation(point.x - bounds.width/2, point.y - bounds.height/2);
      }
   }

   private Rectangle actualBounds = new Rectangle();
   // TODO: fix this
   float zoomLevel = 1.0f;
   public void setZoomLevel(float zoomLevel){

      if (true)
         return;

      this.zoomLevel = zoomLevel;

      for (SikuliGuideComponent sklComp : getFollowers()){
         if (sklComp.autolayout != null){
            sklComp.setZoomLevel(zoomLevel);
         }
      }

      Debug.info("[setZoomLevel] Component:" + this);
//      Debug.info("Actual bounds:" + actualBounds);
      Rectangle bounds = new Rectangle(getActualBounds());

      bounds.x *= zoomLevel;
      bounds.y *= zoomLevel;
      bounds.width *= zoomLevel;
      bounds.height *= zoomLevel;

      //super.setBounds(bounds);
      super.setBounds(bounds);

      for (SikuliGuideComponent sklComp : getFollowers()){
         if (sklComp.autolayout != null){

            Debug.info("Updaing by offset:" + sklComp.autolayout);
            Debug.info("Updaing child:" + sklComp);


            if (sklComp.autolayout instanceof AutoLayoutByMovement){
               ((AutoLayoutByMovement) sklComp.autolayout).x = bounds.x;
               ((AutoLayoutByMovement) sklComp.autolayout).y = bounds.y;
            } else if (sklComp.autolayout instanceof AutoLayoutByOffset){
//               ((AutoLayoutByOffset) sklComp.autolayout).offsetx *= zoomLevel;
//               ((AutoLayoutByOffset) sklComp.autolayout).offsety *= zoomLevel;
//               sklComp.zoomLevel = zoomLevel;
               sklComp.autolayout.update();
            } else{
               sklComp.autolayout.update();
            }
         }
      }


   }


   class Margin{
      int top;
      int left;
      int bottom;
      int right;
   }
   Margin margin = null;
   public void setMargin(int top, int left, int bottom, int right){
      margin = new Margin();
      margin.top = top;
      margin.left = left;
      margin.bottom = bottom;
      margin.right = right;
   }

   int offsetx = 0;
   int offsety = 0;
   public void setOffset(int offsetx, int offsety){
      this.offsetx = offsetx;
      this.offsety = offsety;
   }

   public int getActualWidth(){
      return getActualBounds().width;
   }

   public int getActualHeight(){
      return getActualBounds().height;
   }

   public void setLocationRelativeToRegion(Region region, Layout side) {

      if (margin != null){
         Region rectWithSpacing = new Region(region);
         rectWithSpacing.x -= margin.left;
         rectWithSpacing.y -= margin.top;
         rectWithSpacing.w += (margin.left + margin.right);
         rectWithSpacing.h += (margin.top + margin.bottom);
         region = rectWithSpacing;
      }

      region.x += offsetx;
      region.y += offsety;

      int height = getActualHeight();
      int width = getActualWidth();

      if (side == Layout.TOP){
         setActualLocation(region.x + region.w/2 - width/2, region.y - height);
      } else if (side == Layout.BOTTOM){
         setActualLocation(region.x + region.w/2 - width/2, region.y + region.h);
      } else if (side == Layout.LEFT){
         setActualLocation(region.x - width, region.y + region.h/2 - height/2);
      } else if (side == Layout.RIGHT){
         setActualLocation(region.x + region.w, region.y + region.h/2 - height/2);
      } else if (side == Layout.INSIDE){
         setActualLocation(region.x + region.w/2 - width/2, region.y + region.h/2 - height/2);
      } else if (side == Layout.OVER){
         setActualBounds(region.getRect());
      } else if (side == Layout.ORIGIN){
         setActualLocation(region.x,region.y);
      }

   }


   public void setHorizontalAlignmentWithRegion(Region region, float f){

      int x0 = region.x;
      int x1 = region.x + region.w - getActualWidth();

      int x = (int) (x0 + (x1-x0)*f);

      setActualLocation(x,getActualLocation().y);
   }

   public void setVerticalAlignmentWithRegion(Region region, float f){

      int y0 = region.y;
      int y1 = region.y + region.h - getActualHeight();

      int y = (int) (y0 + (y1-y0)*f);

      setActualLocation(getActualLocation().x,y);
   }


   private ArrayList<SikuliGuideComponent> followers = new ArrayList<SikuliGuideComponent>();
   SikuliGuideComponent leader;

   public void removeFromLeader(){
      if (leader != null)
         leader.removeFollower(this);
      leader = null;
   }

   public void addFollower(SikuliGuideComponent sklComp){
      // force the follower to have the same visibility
      sklComp.setVisible(isVisible());
      sklComp.setOpacity(opacity);

      if (followers.indexOf(sklComp)<0){
         // if this component is not already a follower

         // add it to the list of follower
         followers.add(sklComp);

         // remove its previous leader
         sklComp.removeFromLeader();

         // set its new leader to self
         sklComp.leader = this;
      }
   }

   private void updateAllFollowers(){
      for (SikuliGuideComponent sklComp : getFollowers()){
         if (sklComp.autolayout != null){
            sklComp.autolayout.update();
         }
      }
   }

   @Override
   public void setVisible(boolean visible){
      for (SikuliGuideComponent follower : getFollowers()){
         follower.setVisible(visible);
      }
      super.setVisible(visible);
   }

//   @Override
//   public void setLocation(Point location){
//      setLocation(location.x, location.y);
//   }

//   @Override
//   public void setLocation(int x, int y){
//
////      if (shadowRenderer != null){
////         x -= 8;
////         y -= 8;
////      }
//
//      getActualBounds().x = (int) (x/zoomLevel);
//      getActualBounds().y = (int) (y/zoomLevel);
//
//      super.setLocation(x,y);
//      updateAllFollowers();
//   }
//
//   @Override
//   public void setBounds(int x, int y, int w, int h){
//
//      Rectangle bounds = new Rectangle(x,y,w,h);
//
//      actualBounds = new Rectangle(bounds);
//      actualBounds.x /= zoomLevel;
//      actualBounds.y /= zoomLevel;
//      actualBounds.width /= zoomLevel;
//      actualBounds.height /= zoomLevel;
//
//      for (SikuliGuideComponent sklComp : getFollowers()){
//         if (sklComp.autolayout != null){
//            sklComp.autolayout.update();
//         }
//      }
//      super.setBounds(x,y,w,h);
//   }

//   @Override
//   public void setBounds(Rectangle bounds){
//
//      setActualBounds(new Rectangle(bounds));
//      getActualBounds().x /= zoomLevel;
//      getActualBounds().y /= zoomLevel;
//      getActualBounds().width /= zoomLevel;
//      getActualBounds().height /= zoomLevel;
//
//      super.setBounds(bounds);
//      updateAllFollowers();
//   }

   boolean hasShadow(){
      return shadowRenderer != null;
   }

   public void setActualLocation(Point location){
      setActualLocation(location.x, location.y);
   }

   public void setActualLocation(int x, int y){

      int paintX = x;
      int paintY = y;

      actualBounds.setLocation(x,y);

      if (hasShadow()){
         paintX -= (shadowSize-shadowOffset);
         paintY -= (shadowSize-shadowOffset);
      }

      super.setLocation(paintX, paintY);
      updateAllFollowers();
   }

   public void setActualSize(int width, int height){
      setActualSize(new Dimension(width,height));
   }

   public void setActualSize(Dimension actualSize){

      actualBounds.setSize(actualSize);

      Dimension paintSize = (Dimension) actualSize.clone();

      if (hasShadow()){
         paintSize.width += (2*shadowSize);
         paintSize.height += (2*shadowSize);
      }
      super.setSize(paintSize);
      updateAllFollowers();
   }

   public void setActualBounds(Rectangle actualBounds) {
      this.actualBounds = (Rectangle) actualBounds.clone();

      Rectangle paintBounds = (Rectangle) actualBounds.clone();
      if (hasShadow()){
         paintBounds.x -= (shadowSize-shadowOffset);
         paintBounds.y -= (shadowSize-shadowOffset);
         paintBounds.width += (2*shadowSize);
         paintBounds.height += (2*shadowSize);
      }

      super.setBounds(paintBounds);
      updateAllFollowers();
   }


//   @Override
//   public void setSize(int width, int height){
//      getActualBounds().width = (int) (width/zoomLevel);
//      getActualBounds().height = (int) (height/zoomLevel);
//
//      if (hasShadow()){
//         width += 20;
//         height += 20;
//      }
//
//      super.setSize(width, height);
////      updateAllFollowers();
//   }
//
//   @Override
//   public void setSize(Dimension size){
////      getActualBounds().width = (int) (size.width/zoomLevel);
////      getActualBounds().height = (int) (size.height/zoomLevel);
//
////      if (hasShadow()){
////         size.width += 20;
////         size.height += 20;
////      }
//
//      super.setSize(size);
////      updateAllFollowers();
//   }


   public Point getCenter(){
      Point loc = new Point(getActualLocation());
      Dimension size = getActualSize();
      loc.x += size.width/2;
      loc.y += size.height/2;
      return loc;
   }

   public ArrayList<SikuliGuideComponent> getFollowers() {
      return followers;
   }

   public SikuliGuideComponent getLeader() {
      return leader;
   }

   public void removeFollower(SikuliGuideComponent comp) {
      followers.remove(comp);
   }

   public void setAutoLayoutEnabled(boolean autoLayoutEnabled) {
      this.autoLayoutEnabled = autoLayoutEnabled;
   }

   public boolean isAutoLayoutEnabled() {
      return autoLayoutEnabled;
   }

   public void setAutoResizeEnabled(boolean autoResizeEnabled) {
      this.autoResizeEnabled = autoResizeEnabled;
   }

   public boolean isAutoResizeEnabled() {
      return autoResizeEnabled;
   }

   public void setAutoMoveEnabled(boolean autoMoveEnabled) {
      this.autoMoveEnabled = autoMoveEnabled;
   }

   public boolean isAutoMoveEnabled() {
      return autoMoveEnabled;
   }

   public void removeFrom(Container container) {
      for (SikuliGuideComponent follower : getFollowers()){
         follower.removeFrom(container);
      }
      container.remove(this);
   }

   public void setAutoVisibilityEnabled(boolean autoVisibilityEnabled) {
      this.autoVisibilityEnabled = autoVisibilityEnabled;
   }

   public boolean isAutoVisibilityEnabled() {
      return autoVisibilityEnabled;
   }

   public Rectangle getFollowerBounds(){

      // find the total bounds of all the components
      Rectangle bounds = new Rectangle(getBounds());
      for (SikuliGuideComponent sklComp : getFollowers()){
         bounds.add(sklComp.getBounds());
      }
      return bounds;
   }


   public Rectangle getActualBounds() {
      return actualBounds;
   }

   public Point getActualLocation(){
      return actualBounds.getLocation();
   }

   public Dimension getActualSize() {
      return new Dimension(getActualWidth(),getActualHeight());
   }


   public void addAnimationListener(AnimationListener listener){
      animationListener = listener;
   }

   AnimationListener animationListener;
   public void animationCompleted(){
      if (animationListener != null)
         animationListener.animationCompleted();
   }

}