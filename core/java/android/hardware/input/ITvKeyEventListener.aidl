/*
 * add interceptKeyBeforeDispatchingByTv
 * 
 */

package android.hardware.input;
import android.view.KeyEvent;

interface ITvKeyEventListener {

   boolean interceptKeyBeforeDispatchingByTv(in KeyEvent event, int policyFlags);
               
}
