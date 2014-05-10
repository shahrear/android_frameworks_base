
package android.net.ethernet;

/**
 * @hide
 */
interface IEthernetManager
{
    int getEthernetIfaceState();

    int getEthernetCarrierState();
	
    int getEthernetConnectState();

    boolean setEthernetEnabled(boolean enable);
    
    String getEthernetIfaceName();
}

