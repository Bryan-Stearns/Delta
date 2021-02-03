package edu.umich.eecs.soar.delta;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is for representing parsed text from a lisp-style file.
 * Each object can be either a single String, or a list of strings, corresponding to the tokens within a parenthesis block.
 * @author Bryan Stearns 
 * @since Oct 2020
 */
public class LispishObject {
	private String data;
	private LispishObject parentObject = null;
	private ArrayList<LispishObject> dataList;
	
	public LispishObject(String str) {
		data = str;
		parentObject = null;
		dataList = null;
	}
	public LispishObject() {
		data = null;
		parentObject = null;
		dataList = null;
	}
	
	public String getData() { return data; }
	public LispishObject getParent() { return parentObject; }
	public ArrayList<LispishObject> getDataList() { return dataList; }
	public boolean isList() { return (dataList != null); }
	public boolean isEmpty() { return (data == null && dataList == null); }
	
	public int size() { 
		if (dataList != null)
			return dataList.size();
		else if (data != null)
			return 1;
		return 0;
	}
	
	/**
	 * Clears all contained data references
	 */
	public void clear() {
		data = null;
		
		if (dataList != null)
			dataList.clear();
		else
			dataList = null;
	}
	
	public String getString(int index) throws ArrayIndexOutOfBoundsException {
		if (index == 0 && dataList == null) {
			return data;
		}
		else if (dataList == null || dataList.size() < index || index < 0) {
			int sz = 0;
			if (dataList != null) {
				sz = dataList.size();
			}
			throw new ArrayIndexOutOfBoundsException("Invalid index " + index + " for LispishObject. Size is " + sz + ".");
		}
		
		return dataList.get(index).toString();
	}
	
	/**
	 * Get the LispishObject at the given index within this object.
	 * If there is no object at the given index, throws OutOfBoundsException.
	 * @param index The index of the requested object, where 0 is the first index
	 * @return A LispishObject referenced at the given index within this object
	 * @throws ArrayIndexOutOfBoundsException
	 */
	public LispishObject get(int index) throws ArrayIndexOutOfBoundsException {
		if (dataList == null) {
			throw new ArrayIndexOutOfBoundsException("Cannot get LispishObject at index " + index + ". The object is either empty or has only a single string.");
		}
		else if (dataList.size() < index || index < 0) {
			int sz = dataList.size();
			throw new ArrayIndexOutOfBoundsException("Invalid index " + index + " in LispishObject data list. Size is " + sz + ".");
		}
		
		return dataList.get(index);
	}
	
	/**
	 * Add an empty object to the data list and return its reference.
	 * This action also converts any existing non-list String data into list form, and the new object is then appended to the list.
	 * @return the new LispishObject instance
	 */
	public LispishObject addObject() {
		if (dataList == null)
			dataList = new ArrayList<LispishObject>(3);
		
		// If there is a single String data so far only, convert that to the first list element
		if (data != null) {
			dataList.add(new LispishObject(data));
			data = null;
		}
		
		LispishObject retVal = new LispishObject();
		dataList.add(retVal);
		retVal.parentObject = this;
		
		return retVal;
	}
	
	/**
	 * Add an object to the data list with the given string as its first item, and return the reference to the new object.
	 * This action also converts any existing non-list String data into list form, and the new object is then appended to the list.
	 * @param str The String to add as the first item of the new object
	 * @return The new LispishObject instance
	 */
	public LispishObject addObject(String str) {
		LispishObject retval = addObject();
		retval.addString(str);
		return retval;
	}
	
	/**
	 * Set the contents of this object to a single string.
	 * This overwrites any existing data in this object.
	 * @param str
	 */
	public void setSingleData(String str) {
		data = str;
		
		if (dataList != null)
			dataList.clear();
		else
			dataList = null;
	}
	
	/**
	 * Set the contents of this object to be a list of strings corresponding to the given ArrayList of Strings.
	 * This overwrites any existing data in this object. 
	 * @param strs
	 */
	public void setListData(ArrayList<String> strs) {
		// Configure
		if (dataList != null) {
			dataList.clear();
			data = null; // Should already be null if dataList isn't, but clear it to be safe
		}
		else {
			dataList = new ArrayList<LispishObject>(3);
			data = null;
		}
		
		// Make a list of objects corresponding to each item in the String list
		for (String str : strs) {
			LispishObject obj = new LispishObject(str);
			dataList.add(obj);
			obj.parentObject = this;
		}
	}
	
	/**
	 * Add a String to the object in list form.
	 * If there is already single non-list String data here, it will become the first list item, and the new string will be the second item. 
	 * @param str
	 */
	public void addString(String str) {
		// Create the list if currently empty
		if (dataList == null) {
			dataList = new ArrayList<LispishObject>(3);
		}
		
		// If there is a single String data so far only, convert that to the first list element
		if (data != null) {
			LispishObject obj = new LispishObject(data);
			obj.parentObject = this;
			dataList.add(obj);
			data = null;
		}
		
		// Add the new String
		LispishObject obj = new LispishObject(str);
		obj.parentObject = this;
		dataList.add(obj);
	}
	
	public String getSmemVarName() {
		return "<wm-" + String.valueOf(this.hashCode()) + ">";
	}
	
	/**
	 * Get an ascending list of the indices in this object's data list whose first string matches the given string.
	 * For example, if this object was "(foo bar berry (bar ...) cherry)" and the pattern was "bar", return [1,3]
	 * @param pattern The string to match
	 * @return The list of indices in which the pattern was found
	 */
	public List<Integer> getNamedSublistIndices(String pattern) {
		List<Integer> retval = new ArrayList<Integer>();
		
		// Check if there is a list to search
		if (!isList()) {
			// If not, just check the single string data
			if (data == null) {
				return retval;
			}
			if (data.equals(pattern)) {
				retval.add(0);
			}
			return retval;
		}
		
		// Iterate and search
		for (int i=0; i<dataList.size(); ++i) {
			if (dataList.get(i).getString(0).equals(pattern)) {
				retval.add(i);
			}
		}
		
		return retval;
	}
	
	/**
	 * Returns a string suitable to include inside an "smem --add" command.
	 * This method is recursive. The returned string represents both the given object and its descendants.
	 * @return
	 */
	public String toSmemString() {
		// Terminating string:
		if (!isList()) {
			return data;
		}
		
		// Is a list: print as its own object
		String varName = getSmemVarName();
		String retval = "(" + varName;
		
		for (LispishObject obj : dataList) {
			if (obj.isList()) 		// TODO
			retval += "\t^child " + obj.getSmemVarName();
		}
		
		return retval;
	}
	
	@Override
	public String toString() {
		if (data != null) {
			return data;
		}
		else if (dataList == null) {
			return "NULL";
		}
		else if (dataList.size() == 0) {
			return "";
		}
		
		String retval = " (";
		for (LispishObject obj : dataList) {
			if (obj.isList())
				retval += " (...)";
			else
				retval += " " + obj.toString();
		}
		retval += " )";
		
		
		return retval;
	}
}
