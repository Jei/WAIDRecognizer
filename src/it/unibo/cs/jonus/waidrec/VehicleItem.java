/**
 * 
 */
package it.unibo.cs.jonus.waidrec;

import android.graphics.Bitmap;

/**
 * @author jei
 *
 */
public class VehicleItem {
	
	private String category;
	private Bitmap icon;

	/**
	 * 
	 */
	public VehicleItem() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @return the category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * @param category the category to set
	 */
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * @return the icon
	 */
	public Bitmap getIcon() {
		return icon;
	}

	/**
	 * @param icon the icon to set
	 */
	public void setIcon(Bitmap icon) {
		this.icon = icon;
	}

}
