package gob.cinvestav.mx.pte.ws;

import java.util.ArrayList;
import java.util.List;

public class EntityOpts {
	/*
	 * (i) 		extract AlchemyEntities 
	 * (ii) 	extract BabelfyEntities 
	 * (iii) 	fuse uris
	 */
	
	//(i)
	public static List<AlchemyEntities> extractAlchemyEntities(String sentence) {
		List<AlchemyEntities> alchemy;
		alchemy = new AlchemyWS().extractEntities(sentence);
		return alchemy;
	}
	
	//(ii)
	public static List<BabelfyEntities> extractBabelfyEntities(String sentence) {
		List<BabelfyEntities> babelfy = null;

		try {
			babelfy = new BabelfyWS().extractEntities(sentence);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return babelfy;
	}

	//(iii) fuse uris
	public static void fuseAlchemyUris(List<AlchemyEntities> alchemy, List<Entity> entities){
		if(!entities.isEmpty()){
			for(AlchemyEntities alchemyEntity : alchemy){
				Entity entity = new Entity();
				List<String> uris = new ArrayList<String>();
				entity.setText(alchemyEntity.getText());
				if(alchemyEntity.getDbpediaURL().length() > 0)
					uris.add(alchemyEntity.getDbpediaURL());
//				if(alchemyEntity.getFreebaseURL().length() > 0)
//					uris.add(alchemyEntity.getFreebaseURL());
				entity.setUris(uris);
				entities.add(entity);
			}
		}else{
			for(AlchemyEntities alchemyEntity : alchemy){
				if(!entities.contains(alchemyEntity.getText())){
					Entity entity = new Entity();
					List<String> uris = new ArrayList<String>();
					entity.setText(alchemyEntity.getText());
					if(alchemyEntity.getDbpediaURL().length() > 0)
						uris.add(alchemyEntity.getDbpediaURL());
//					if(alchemyEntity.getFreebaseURL().length() > 0)
//						uris.add(alchemyEntity.getFreebaseURL());
					entity.setUris(uris);
					entities.add(entity);
				}
			}
		}
	}
	
	public static void fuseBabelfyUris(List<BabelfyEntities> babelfy, List<Entity> entities){
		if(!entities.isEmpty()){
			for(BabelfyEntities babelfyEntity : babelfy){
				Entity entity = new Entity();
				List<String> uris = new ArrayList<String>();
				entity.setText(babelfyEntity.getText());
//				if(babelfyEntity.getDbpediaURL().length() > 0)
				if(babelfyEntity.getDbpediaURL() != null)
					uris.add(babelfyEntity.getDbpediaURL());
//				if(babelfyEntity.getBabelNetURL().length() > 0)
//					uris.add(babelfyEntity.getBabelNetURL());
				entity.setUris(uris);
				entities.add(entity);
			}
		}else{
			for(BabelfyEntities babelfyEntity : babelfy){
				if(!entities.contains(babelfyEntity.getText())){
					Entity entity = new Entity();
					List<String> uris = new ArrayList<String>();
					entity.setText(babelfyEntity.getText());
//					if(babelfyEntity.getDbpediaURL().length() > 0)
					if(babelfyEntity.getDbpediaURL() != null)
						uris.add(babelfyEntity.getDbpediaURL());
//					if(babelfyEntity.getBabelNetURL().length() > 0)
//						uris.add(babelfyEntity.getBabelNetURL());
					entity.setUris(uris);
					entities.add(entity);
				}
			}
		}
	}
	
	public static void printEntities(List<Entity> entities){
		for(Entity entity : entities){
			if(!entity.getUris().isEmpty()){
				System.out.println("Text = " + entity.getText());
				System.out.println("Uris = " + entity.getUris());
			}
		}
	}
}
