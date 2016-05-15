package asana;

import java.io.*;
import java.net.*;
import java.util.*;

import com.asana.*;
import com.asana.models.*;

public class Main {
	
	public static void main(String[] args) throws IOException, URISyntaxException{
		
        // create an OAuth client with the app
        Client client = Client.basicAuth("0/565bdbbac039b153dbd778c33310b153");
        
        Workspace accenture = null;
        for (Workspace workspace : client.workspaces.findAll()) {
            if (workspace.name.equals("accenture.com")) {
                accenture = workspace;
                break;
            }
        }
        
        List<Project> projects = client.projects.findByWorkspace(accenture.id).execute();
        Project generalTasksProject = null;
        for (Project project : projects) {
            if (project.name.equals("General Tasks")) {
                generalTasksProject = project;
                break;
            }
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
        
        List<String> projectArray = Arrays.asList(generalTasksProject.id);
        
        Task bsr = client.tasks.createInWorkspace(accenture.id)
        		.data("name", "BSR")
        		.data("projects", projectArray)
        		.execute();
        
        createSubtasks(client, accenture, buildTask, bsr);

	}
	
	static void createSubtasks (Client client, Workspace workspace, Task template, Task newTask) throws IOException {
		
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
	            
	            createSubtasks(client, workspace, task, createdTask);
	        }
        }
	}
}
