/*
 *   EuroCarbDB, a framework for carbohydrate bioinformatics
 *
 *   Copyright (c) 2006-2009, Eurocarb project, or third-party contributors as
 *   indicated by the @author tags or express copyright attribution
 *   statements applied by the authors.  
 *
 *   This copyrighted material is made available to anyone wishing to use, modify,
 *   copy, or redistribute it subject to the terms and conditions of the GNU
 *   Lesser General Public License, as published by the Free Software Foundation.
 *   A copy of this license accompanies this distribution in the file LICENSE.txt.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *   or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 *   for more details.
 *
 *   Last commit: $Rev$ by $Author$ on $Date::             $  
 */
/**
 @author David Damerell (d.damerell@imperial.ac.uk)
 */
package org.eurocarbdb.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class Combinator{
	HashSet<String> combinations=new HashSet<String>();
	
	public void generate(String word){
		combinations.add(word);
		
		if(word.length()==1){
			return;
		}
		
		for(int i=0;i<word.length();i++){
			generate(word.substring(0,i)+word.substring(i+1));
		}
	}
	
	public static <T> HashSet<List<T>> generate(List<T> list){
		HashSet<List<T>> combinations=new HashSet<List<T>>();
		
		generate(list, combinations);
		
		return combinations;
	}
	
	public static <T> void generate(List<T> list, HashSet<List<T>> combinations){
		combinations.add(list);
		
		if(list.size()==1){
			return;
		}
		
		for(int i=0;i<list.size();i++){
			List<T> newList=new ArrayList<T>();
			newList.addAll((Collection<? extends T>) list.subList(0, i));
			newList.addAll((Collection<? extends T>) list.subList(i+1, list.size()));
			
			generate(newList,combinations);
		}
	}
	
	public static <T> HashSet<List<T>> generate(List<T> list, List<T> sequence){
		HashSet<List<T>> combinations=new HashSet<List<T>>();
		
		generate(list,sequence,combinations,0);
		
		return combinations;
	}
	
	public static <T> void generate(List<T> list, List<T> sequence, HashSet<List<T>> combinations,int i){
		if(i==sequence.size()){
			combinations.add(sequence);
			return;
		}
		
		for(T obj:list){
			System.err.println(sequence.size());
			
			List<T> cloneOfSequence=new ArrayList<T>(sequence);
			cloneOfSequence.set(i, obj);
			
			generate(list,cloneOfSequence,combinations,i+1);
		}
	}
	
	
	public static void main(String args[]){
		{
			List<Integer> list=new ArrayList<Integer>();
			list.add(0);
			list.add(1);
			list.add(2);
			list.add(3);
			list.add(4);
			list.add(5);
			list.add(6);
			list.add(7);
			list.add(8);

			HashSet<List<Integer>> combinations=Combinator.generate(list);
			for(List<Integer> combination:combinations){
				System.out.println(combination.toString());
			}
		}
		
		{
			List<String> list2=new ArrayList<String>();
			list2.add("A");
			list2.add("B");
			list2.add("C");

			List<String> sequence=new ArrayList<String>();
			sequence.add("");
			sequence.add("");
			sequence.add("");

			HashSet<List<String>> combinations2=Combinator.generate(list2,sequence);
			for(List<String> combination:combinations2){
				System.out.println(combination.toString());
			}
		}
		
		{
			List<String> list3=new ArrayList<String>();
			list3.add("Na");
			list3.add("Na");
			list3.add("H");

			HashSet<List<String>> combinations=Combinator.generate(list3);
			for(List<String> combination:combinations){
				System.out.println(combination.toString());
			}
		}
	}
}
