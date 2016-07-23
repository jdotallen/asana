package asana;

import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.asana.*;
import com.asana.models.*;
import com.asana.requests.ItemRequest;
import com.mashape.unirest.http.*;
import com.mashape.unirest.http.exceptions.UnirestException;

public class Main {
	
	static Client 			client 		= null;
	static Workspace 		accenture 	= null;
	static List<Project> 	projects	= null;
	
	public static void main(String[] args) throws IOException, URISyntaxException, ParseException, UnirestException {
		
		if ( args.length == 0 ) {
			System.out.println("Please provide input!!");
			return;
		} else {
		
		    for ( int i = 0 ; i < args.length ; i++ ){
		    	System.out.println(args[i]);
		    }
			setup( );
			
			switch (args[0]) {
			
				case "BSR":
					createBSR( args[1] );
					break;
					
				case "RollIn":
					//RollIn [EID] [New Hire? (YES|NO)] [RollIn Date YYYY-MM-DD]
					createRollInChecklist( args[1], args[2], args[3] );
					break;
					
				case "Task":
					createTask( args[1] );
					break;
		
				default:
					System.out.println( "Please provide valid input!");
					break;
					
			}
		
		}
	

	}
	
	static void createTask( String taskName ) throws IOException, ParseException {
        List<String> projectArray = Arrays.asList("36058796460797");
        
        Task old = client.tasks.findById("114116490790539").execute();
        
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date date = format.parse("2017-01-01");
        
       
		Task bsr = client.tasks.createInWorkspace(accenture.id)
        		.data("name", taskName )
        		.data("notes", old.notes)
        		.data("projects", projectArray)
        		.data("due_on", format.format(date))
        		.execute();

        System.out.println(bsr.name + " " + bsr.id);
	}
	
	static void createSubtasks (Client client, Task template, Task newTask) throws IOException {
		
		List<Task> subtasks = null;
        subtasks = client.tasks.subtasks(template.id).execute();
        
        if (subtasks != null) {
        	Collections.reverse(subtasks);
	        for (Task task : subtasks) {

	            Task createdTask = client.tasks.create()
	            		.data("name", task.name)
	            		.data("parent", newTask.id)
	            		.data("notes", task.notes)
	            		.execute();

	            System.out.println(createdTask.name + " " + createdTask.id);
	            
	            createSubtasks(client, task, createdTask);
	        }
        }
	}
	
	static void setup ( ) throws IOException {
		
        // create an OAuth client with the app
        client = Client.basicAuth(System.getenv("ASANA_API_KEY"));
        
        for (Workspace workspace : client.workspaces.findAll()) {
            if (workspace.name.equals("accenture.com")) {
                accenture = workspace;
                break;
            }
        }

        projects = client.projects.findByWorkspace(accenture.id).execute();
        
	}
	
	static void createBSR ( String bsrName ) throws IOException, UnirestException {
		
		if (client == null) {
			return;
		}
        
        Project templatesProject = null;
        for (Project project : projects) {
            if (project.name.equals("Templates")) {
                templatesProject = project;
                break;
            }
        }        
        
        List<Task> templates = client.tasks.findByProject(templatesProject.id).execute();
        Task buildTask = null;
        for (Task task : templates) {
        	if (task.name.equals("Regular Build Lifecycle")) {
        		buildTask = task;
        		break;
        	}
        }

        Project bsrsProject = null;
        for (Project project : projects) {
            if (project.name.equals("BSRs")) {
                bsrsProject = project;
                break;
            }
        }
        
        List<String> projectArray = Arrays.asList(bsrsProject.id);
        
        Task bsr = client.tasks.createInWorkspace(accenture.id)
        		.data("name", bsrName)
        		.data("projects", projectArray)
        		.execute();
        

		HttpResponse<JsonNode> jsonResponseCreate = Unirest.post("https://slack.com/api/channels.create")
		  .header("accept", "application/json")
//		  .queryString("apiKey", "123")
		  .field("token", System.getenv("SLACK_API_KEY"))
		  .field("name", "bsr" + bsrName.substring(4, 8))
		  .asJson();
//		HttpResponse<JsonNode> jsonResponse = Unirest.post("https://slack.com/api/channels.info")
//				  .header("accept", "application/json")
////				  .queryString("apiKey", "123")
//				  .field("token", "xoxp-2403598953-2403598955-61889181778-29dbbae262")
//				  .field("channel", "C1UEKA9MW")
//				  .asJson();
		System.out.println(jsonResponseCreate.getBody().toString());
		String id = jsonResponseCreate.getBody().getObject().getJSONObject("channel").getString("id");
		System.out.println(id);
		HttpResponse<JsonNode> jsonResponseTopic = Unirest.post("https://slack.com/api/channels.setTopic")
				  .header("accept", "application/json")
//				  .queryString("apiKey", "123")
				  .field("token", System.getenv("SLACK_API_KEY"))
				  .field("channel", id)
				  .field("topic", "https://app.asana.com/0/" + bsrsProject.id + "/" + bsr.id)
				  .asJson();
		System.out.println(jsonResponseTopic.getBody().toString());
        
        
        createSubtasks(client, buildTask, bsr);
	}

	static void createRollInChecklist ( String EID, String newHire, String rollInDate ) throws IOException, ParseException {

		if (client == null) {
			return;
		}

        Project rollinProject = null;
        for (Project project : projects) {
            if (project.name.equals("Roll-In Checklist - Template v5")) {
                rollinProject = project;
                break;
            }
        }
        
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date date = format.parse(rollInDate);
        
        List<Task> rollin = client.tasks.findByProject(rollinProject.id).execute();
        Collections.reverse(rollin);
        
        List<Team> teams = client.teams.findByOrganization(accenture.id).execute();
        
        Team manila = null;
        for (Team team : teams) {
        	if (team.name.equals("A-MNL")) {
        		manila = team;
        		break;
        	}
        }
        

    	// get roles
    	Project rolesProject = null;
        for (Project project : projects) {
            if (project.name.equals("Roles and Responsibilities")) {
                rolesProject = project;
                break;
            }
        }
        
        List<Task> roles = client.tasks.findByProject(rolesProject.id).execute();
        
        Project newRollinProject = client.projects.createInTeam(manila.id)
        									.data("name", "Roll-In Checklist - " + EID)
        									.execute();
        
        System.out.println(newRollinProject.name + " - " + newRollinProject.id);
        
        List<String> projectArray = Arrays.asList(newRollinProject.id);
//        List<String> projectArray = Arrays.asList("141948861337807");
        ArrayList<String> tagsArray = null;
        
        for (Task task : rollin ) {
        	
//        	if (task.name.equals("Tests:")) {
//        		break;
//        	}
        	
        	task = client.tasks.findById(task.id).execute();
        	
        	ItemRequest<Task> taskRequest = client.tasks.createInWorkspace(accenture.id)
        			.data("name", task.name)
        			.data("notes", task.notes)
        			.data("projects", projectArray);
        	
        	// get date variable
        	int dateVar = 0;
        	try {
        		int from = task.name.lastIndexOf('[');
        		int to = task.name.lastIndexOf(']');
        		String sub = task.name.substring(from + 1, to);
        		dateVar = Integer.decode(sub);
        		System.out.println(dateVar);
        	} catch (Exception ex) {
        		dateVar = 99;
        	}
        	
        	// assign due date
        	if (dateVar < 99) {
        		Calendar cal = Calendar.getInstance();
        		cal.setTime(date);
        		cal.add(Calendar.DAY_OF_MONTH, dateVar);
        		Date newDate = cal.getTime();
        		
        		taskRequest.data("due_on", format.format(newDate));
        	}
        	
        	
        	// Get tags
        	if (task.tags != null) {
        	
        		tagsArray = new ArrayList<String>();
        		Iterator<Tag> it = task.tags.iterator();
        		
        		while(it.hasNext()) {
        			Tag tag = it.next();
        			tagsArray.add(tag.id);
                	
        			String assigneeID = null;
                    for (Task role : roles) {
                    	
                    	if (tag.name.equals("Contractor") ||
                    	    tag.name.equals("N/A")) {
                    		assigneeID = null;
                    		break;
                    	} else if (tag.name.equals("New Hire") && newHire.contains("N")) {
                    		assigneeID = null;
                    		break;
                    	} else if (role.name.equals(tag.name)) {
                    		role = client.tasks.findById(role.id).execute();
                    		assigneeID = role.assignee.id;
                    	} else if (tag.name.equals("New Member")) {
                    		assigneeID = EID + "@accenture.com";
                    	}
                    }
                    if (assigneeID != null) {
                    	taskRequest.data("assignee", assigneeID);
                    }
                    
        		}
        		
        		taskRequest.data("tags", tagsArray);
        	}
    		
        			
        	Task newTask = taskRequest.execute();
            System.out.println(newTask.name + " " + newTask.id);
        	
        	createSubtasks(client, task, newTask);
        }
		
	}
	

}
