package main.mybatis.tutorial;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;

public class CategoryBO {
	
	static List<Map<String,List<CategoryVO>>> listCategories = new ArrayList<Map<String,List<CategoryVO>>>();
	
	public List<CategoryVO> getInformation(CategoryVO cat) throws Exception{
		SqlSession session = ConnectionFactory.getSession().openSession();
		CategoryDAO dao = session.getMapper(CategoryDAO.class);
		List<CategoryVO> categories = dao.getInformation(cat);
		session.close();
		return categories;
	}
	
	public static void main(String[] args) throws Exception{
		CategoryBO bo = new CategoryBO();
		CategoryVO vo = new CategoryVO();
		
		CategoryVO cat = new CategoryVO();
		
		cat.setCat_title("computer_science");
		//subcat or page
		cat.setCl_type("subcat");
		List<CategoryVO> catVO = bo.getInformation(cat);
		
//		for(CategoryVO cats : catVO){
//			System.out.println("=================================");
//			System.out.println("cl_from: " + cats.getCl_from());
//			System.out.println("cat_title: " + cats.getCat_title());
//			System.out.println("cl_sortedkey: " + cats.getCl_sortkey());
//			System.out.println("cat_pages: " + cats.getCat_pages());
//			System.out.println("cat_subcats: " + cats.getCat_subcats());
//			System.out.println("cl_type: " + cats.getCl_type());
//		}
		
		bo.retrieveCats("computer_science",catVO);
		bo.printListCategories();
		bo.printCategoriesToFile();
	}
	
	public void retrieveCats(String category,List<CategoryVO> categories){
		Map<String,List<CategoryVO>> mapCats = new HashMap<String,List<CategoryVO>>();
		CategoryBO bo = new CategoryBO();

		for(CategoryVO cats : categories){
			System.out.println("processing category: " + cats.getCl_sortkey());
			CategoryVO cat = new CategoryVO();
			cat.setCat_title(cats.getCl_sortkey());
			cat.setCl_type("subcat");
			
			try {
				List<CategoryVO> listCats = bo.getInformation(cat);
				if(!listCats.isEmpty()){
					System.out.println("sending category to be precessed: " + cats.getCl_sortkey());
					mapCats.put(cats.getCl_sortkey(), listCats);
					retrieveCats(cats.getCl_sortkey(),listCats);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		System.out.println("\n\n The number of maps add to the list is: " + mapCats.size());
		listCategories.add(mapCats);
	}
	
	public void printListCategories(){
		for(Map<String,List<CategoryVO>> mapCategories : listCategories){
			for(String category : mapCategories.keySet()){
				System.out.println("Category = " + category);
				System.out.print("\tSubcategories: ");
				for(CategoryVO catVO : mapCategories.get(category)){
					System.out.print(catVO.getCl_sortkey() + "\t");
				}
				System.out.println();
			}
		}
	}
	
	public void printCategoriesToFile(){
		try(PrintWriter pw = new PrintWriter(new FileWriter("categories.txt"))){
			pw.write("Category\tSubcategories\n");
			for(Map<String,List<CategoryVO>> mapCategories : listCategories){
				for(String category : mapCategories.keySet()){
					pw.write(category + "\t");
					for(CategoryVO catVO : mapCategories.get(category)){
						pw.write(catVO.getCl_sortkey() + "\t");
					}
					pw.write("\n");
				}
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}

}
