package HTML;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javafx.concurrent.Task;
import models.AbstractFFModel;
import models.FFModelDualSinglet;
import statics.FFConstants;
import statistics.FFStatistics;

import comparison.ComparisonSummary;

import containers.Chartable;
import containers.HitContainer;
import containers.StatisticsContainer;

/**
 * Generates the HTML Summary page for the sprox fitting
 * @author jkarnuta
 *
 */
public class HTMLGenerator extends Task<Void>{

	private final AbstractFFModel model;
	private final ComparisonSummary compSummary;
	private StringBuilder html;

	private static final String SAVED_FILENAME = "FittingSummary.html";
	private static final String INSERT_ROW_HERE = "$INSERT_ROW_HERE$";
	private static final String TRUNCATION_FORMAT = "#.#####";

	/*Constants used for temporary placeholders in boilerplates*/
	//Boilerplate file names
	private static final String INITIAL_BOILERPLATE = "FFSummary.boilerplate";
	private static final String COMPARISON_SUMMARY_BOILERPLATE = "ComparisonSummary.boilerplate";
	private static final String POPULATION_STATISTICS_BOILERPLATE = "PopulationStatistics.boilerplate";
	private static final String CALCULATED_HITS_BOILERPLATE = "CalculatedHits.boilerplate";

	//initial comments
	private static final String TEMPDATE = "$TEMPDATE$";
	private static final String INIT_FILE_NAME = "$FILE_NAME$";
	private static final String CLASS_NAME = "$CLASS_NAME$";
	private static final String DIRECTORY_PATH = "$DIRECTORY_PATH$";
	private static final String NUMBER_RUNS = "$NUMBER_RUNS$";

	//ComparisonSummary.boilerplate
	private static final String COMPARISON_SUMMARY_PLACEHOLDER = "$COMPARISON_SUMMARY$";
	private static final String COMPARISON_SUCCESS_STYLE = "$COMPARISON_SUCCESS_STYLE$";
	private static final String COMPARISON_SUCCESS = "$COMPARISON_SUCCESS$";
	private static final String NUMBER_PEPTIDES_COMPARED = "$NUMBER_PEPTIDES_COMPARED$";
	private static final String ALL_PEPTIDES_COMPARED_STYLE = "$ALL_PEPTIDES_COMPARED_STYLE$";
	private static final String ALL_PEPTIDES_COMPARED = "$ALL_PEPTIDES_COMPARED$";
	private static final String ADJUSTED_R_SQ_HUERISTIC = "$ADJUSTED_R_SQ_HUERISTIC$";
	private static final String NUMBER_CLEAN = "$NUMBER_CLEAN$";
	private static final String MIDPOINT_HEURISTIC = "$MIDPOINT_HEURISTIC$";
	private static final String NUMBER_SIGNIFICANT = "$NUMBER_SIGNIFICANT$";
	private static final String DIFFERENCE_HEURISTIC_UPPER = "$UPPER_BOUND_PERCENTILE$";
	private static final String DIFFERENCE_HEURISTIC_LOWER = "$LOWER_BOUND_PERCENTILE$";
	private static final String NUMBER_PASSED_DIFFERENCE = "$DIFFERENCE_PASSED$";
	private static final String NUMBER_HITS = "$NUMBER_HITS$";

	//PopulationStatistics.boilerplate
	private static final String POPULATION_STATISTICS_PLACEHOLDER = "$POPULATION_STATISTICS$";

	//CalculatedHits.boilerplate
	private static final String CALCULATED_HITS_PLACEHOLDER = "$CALCULATED_HITS$";

	//AllRuns.boilerplate
	private static final String ALL_RUNS_BOILERPLATE = "AllRuns.boilerplate";
	private static final String ALL_RUNS_PLACEHOLDER = "$ALL_RUNS$";
	private static final String LIGAND_SUP_HEADER ="$LIGAND_SUP_HEADER$";
	private static final String LIGAND_HEADER_REPLACEMENT = "<th colspan = \"2\">Ligand</th>"; 
	private static final String CONTROL_LIGAND_SUB_HEADER = "$CONTROL_LIGAND_SUB_HEADER$";
	private static final String SUB_HEADERS = "<th>C<sub>1/2</sub></th>\n<th>Adjusted R<sup>2</sup></th>";

	public HTMLGenerator(AbstractFFModel model, ComparisonSummary compSummary){
		this.model = model;
		this.compSummary = compSummary;
		html = new StringBuilder();
	}

	@Override
	public Void call() throws Exception {

		// load initial boilerplate
		html.append( loadBoilerplate(INITIAL_BOILERPLATE));

		//Fill in inital comments
		findAndReplace(TEMPDATE, getCurrentDate());
		findAndReplace(INIT_FILE_NAME, model.getSPROXFileName());
		findAndReplace(CLASS_NAME, model.getClass().getName());
		findAndReplace(DIRECTORY_PATH, model.getSuperPath());
		int numberRuns = 0;
		if (compSummary != null){
			numberRuns = compSummary.numberComparedPeptides;
			findAndReplace(NUMBER_RUNS,numberRuns);
		}
		else{
			try{
				numberRuns = model.data.getChartables1().size();
				findAndReplace(NUMBER_RUNS,numberRuns);
			}
			catch (Exception e){
				numberRuns = 1000000;
				findAndReplace(NUMBER_RUNS, "UNKNOWN");
			}
		}


		/**
		 * Begin Parsing to fill in other portions
		 */
		/*Fill out $COMPARISON_SUMMARY$*/ 
		if (compSummary != null){
			final String comparisonSummaryBoilerplate = loadBoilerplate(COMPARISON_SUMMARY_BOILERPLATE);
			findAndReplace(COMPARISON_SUMMARY_PLACEHOLDER, comparisonSummaryBoilerplate); 
			findAndReplace(COMPARISON_SUCCESS_STYLE, "success");
			findAndReplace(COMPARISON_SUCCESS, "Successfully saved "+FFConstants.COMPARISON_FILENAME);
			findAndReplace(NUMBER_PEPTIDES_COMPARED, compSummary.numberComparedPeptides);
			findAndReplace(ALL_PEPTIDES_COMPARED_STYLE, compSummary.allCompared ? "success" : "error");
			findAndReplace(ALL_PEPTIDES_COMPARED, compSummary.allCompared ? "TRUE" : "FALSE");
			findAndReplace(ADJUSTED_R_SQ_HUERISTIC, FFConstants.ADJ_R_SQ_HEURISTIC);
			findAndReplace(NUMBER_CLEAN, compSummary.numberClean);
			findAndReplace(MIDPOINT_HEURISTIC, FFConstants.MIDPOINT_HEURISTIC);
			findAndReplace(NUMBER_SIGNIFICANT, compSummary.numberSignificant);
			if(!FFConstants.RUN_PEPTIDE_ANALYSIS){
				findAndReplace(DIFFERENCE_HEURISTIC_UPPER, "&infin;");
				findAndReplace(DIFFERENCE_HEURISTIC_LOWER, "-&infin;");
			}
			else{
				findAndReplace(DIFFERENCE_HEURISTIC_UPPER, FFConstants.UPPER_BOUND_PERCENTILE);
				findAndReplace(DIFFERENCE_HEURISTIC_LOWER, FFConstants.LOWER_BOUND_PERCENTILE);
			}
			
			findAndReplace(NUMBER_PASSED_DIFFERENCE, compSummary.numberPassedDifferenceAnalysis);
			findAndReplace(NUMBER_HITS, compSummary.numberHits);
		}
		else{
			final String alternateText = "<h2>Comparison Summary</h2>\n<p class = \"error\">Comparison Not Performed</p>";
			findAndReplace(COMPARISON_SUMMARY_PLACEHOLDER, alternateText);
		}

		/*Fill out $POPULATION_STATISTICS$*/
		final String populationStatisticsBoilerplate = loadBoilerplate(POPULATION_STATISTICS_BOILERPLATE);
		findAndReplace(POPULATION_STATISTICS_PLACEHOLDER, populationStatisticsBoilerplate);
		/*calculate population statistics
		 *-b
		 *-C 1/2
		 *-Adjusted R Squared
		 *-A
		 *-B
		 */

		/*
		 * -Control
		 * -Ligand
		 */
		if (compSummary != null){//comparison performed, use control, ligand
			addFullStatisticsBlock("Control ", model.data.getChartables1(), false);

			String nullStatisticsRow = nullStatisticsRowGenerator(true);
			findAndReplace(INSERT_ROW_HERE, nullStatisticsRow);

			addFullStatisticsBlock("Ligand ", model.data.getChartables2(), false);

			findAndReplace(INSERT_ROW_HERE, nullStatisticsRow);

			FFStatistics ffstats = new FFStatistics();
			StatisticsContainer stats = ffstats.getStatistics(compSummary.deltaMidpointList);
			String row = statisticsRowGenerator(false, "&Delta;Midpoint", stats);
			findAndReplace(INSERT_ROW_HERE, row);

		}
		else{
			addFullStatisticsBlock("", model.data.getChartables1(), false);
		}
		findAndReplace(INSERT_ROW_HERE, "");


		/*Fill out $CALCULATED_HITS$ */
		if (compSummary != null){
			//load boilerplate
			final String calculatedHitsBoilerplate = loadBoilerplate(CALCULATED_HITS_BOILERPLATE);
			findAndReplace(CALCULATED_HITS_PLACEHOLDER, calculatedHitsBoilerplate);
			//add rows
			int currentRowNumber = 0;
			for (HitContainer ele : compSummary.hitList){
				String alt = (currentRowNumber % 2 == 1) ? "class = \"alt\"" : "";
				String calculatedHitsRow = calculatedHitsRowGenerator(ele.CSVLineNumber, ele.peptide, 
						ele.protein, alt, model.getGenerateGraphs());
				findAndReplace(INSERT_ROW_HERE, calculatedHitsRow);
				currentRowNumber++;
			}	
			findAndReplace(INSERT_ROW_HERE, "");
		}
		else{
			final String alternateText = "<h2 id = \"calculated-hits\">Calculated Hits</h2>\n"
					+ "<p class = \"error\">Comparison Not Performed</p>";
			findAndReplace(CALCULATED_HITS_PLACEHOLDER, alternateText);
		}

		/*Fill out $ALL_RUNS$*/
		final String allRuns = loadBoilerplate(ALL_RUNS_BOILERPLATE);
		findAndReplace(ALL_RUNS_PLACEHOLDER, allRuns);
		boolean multiple = model instanceof FFModelDualSinglet;
		if(multiple){
			findAndReplace(LIGAND_SUP_HEADER, LIGAND_HEADER_REPLACEMENT);
			findAndReplace(CONTROL_LIGAND_SUB_HEADER,SUB_HEADERS+"\n"+SUB_HEADERS);
		}else{
			findAndReplace(LIGAND_SUP_HEADER, "");
			findAndReplace(CONTROL_LIGAND_SUB_HEADER, SUB_HEADERS);
		}

		int currentRowNumber = 0;
		for (int i = 0; i < model.data.getChartables1().size(); i++){
			String alt = (currentRowNumber % 2 == 1) ? "class = \"alt\"" : "";
			String line;
			if(multiple){
				Chartable c1 = model.data.getChartables1().get(i);
				Chartable c2 = model.data.getChartables2().get(i);
				if(!c1.peptide.equals(c2.peptide)) continue;
				line = allRunsRowGenerator(c1.graphNumber, c1.peptide, c1.protein, c1.chalf, 
						c1.adjRSquared, alt, c2.chalf,c2.adjRSquared, model.getGenerateGraphs());
			}else{
				Chartable c1 = model.data.getChartables1().get(i);
				line = allRunsRowGenerator(c1.graphNumber, c1.peptide, c1.protein, c1.chalf, 
						c1.adjRSquared, alt, model.getGenerateGraphs());
			}
			findAndReplace(INSERT_ROW_HERE, line);
			currentRowNumber++;
		}
		findAndReplace(INSERT_ROW_HERE, "");
		save();
		return null;
	}

	private void save() throws IOException{
		FileWriter fw = new FileWriter(new File(model.getSuperPath()+SAVED_FILENAME));
		fw.write(html.toString());
		fw.flush();
		fw.close();
	}

	private void findAndReplace(String toReplace, double doubleReplacement){
		findAndReplace(toReplace, String.valueOf(doubleReplacement));
	}

	private void findAndReplace(String toReplace, int integerReplacement){
		findAndReplace(toReplace, String.valueOf(integerReplacement));
	}

	private void findAndReplace(String toReplace, String replacement){
		html.replace(html.indexOf(toReplace), html.indexOf(toReplace)+toReplace.length(), replacement);
	}

	private String loadBoilerplate(String resourceName) throws IOException{
		StringBuilder sb = new StringBuilder();
		InputStream is = this.getClass().getResourceAsStream(""+resourceName);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		while ( ( line = br.readLine()) != null){
			sb.append(line+"\n");
		}
		return sb.toString();
	}

	private void addFullStatisticsBlock(String identifier, List<Chartable> data, boolean altStart){
		FFStatistics FFstats = new FFStatistics();

		String name = identifier+"Midpoints";
		StatisticsContainer stats = FFstats.getMidpointStatistics(data);
		String row = statisticsRowGenerator(altStart, name, stats);
		findAndReplace(INSERT_ROW_HERE, row);

		altStart = !altStart;
		name = identifier+"Adjusted R<sup>2</sup>";
		stats = FFstats.getAdjustedRSquaredStatistics(data);
		row = statisticsRowGenerator(altStart, name, stats);
		findAndReplace(INSERT_ROW_HERE, row);

		altStart = !altStart;
		name = identifier+"b";
		stats = FFstats.getbStatistics(data);
		row = statisticsRowGenerator(altStart, name, stats);
		findAndReplace(INSERT_ROW_HERE, row);

		altStart = !altStart;
		name = identifier+"A";
		stats = FFstats.getAStatistics(data);
		row = statisticsRowGenerator(altStart, name, stats);
		findAndReplace(INSERT_ROW_HERE, row);

		altStart = !altStart;
		name = identifier+"B";
		stats = FFstats.getBStatistics(data);
		row = statisticsRowGenerator(altStart, name, stats);
		findAndReplace(INSERT_ROW_HERE, row);
	}

	private String statisticsRowGenerator(boolean alt, String population, StatisticsContainer container){
		return statisticsRowGenerator(alt, population, container.getN(), container.getMean(),
				container.getMedian(), container.getStd(), container.getTenth(), container.getNinetieth());
	}

	private String statisticsRowGenerator(boolean alt,String population, int N, double mean, double median, 
			double standardDeviation, double th10, double th90){
		String classText = alt ? " class = \"alt\"" : "";
		DecimalFormat truncation = new DecimalFormat(TRUNCATION_FORMAT);
		String s =  "<tr"+classText+">"
				+"<td>"+population+"</td>"
				+"<td>"+N+"</td>"
				+"<td>"+truncation.format(mean)+"</td>"
				+"<td>"+truncation.format(median)+"</td>"
				+"<td>"+truncation.format(standardDeviation)+"</td>"
				+"<td>"+truncation.format(th10)+"</td>"
				+"<td>"+truncation.format(th90)+"</td>"
				+"</tr>\n"
				+INSERT_ROW_HERE+"\n";
		return s;
	}

	private String nullStatisticsRowGenerator(boolean alt){
		String classText = alt ? " class = \"alt\"" : "";
		String s =  "<tr"+classText+">"
				+"<td> - </td>"
				+"<td> - </td>"
				+"<td> - </td>"
				+"<td> - </td>"
				+"<td> - </td>"
				+"<td> - </td>"
				+"<td> - </td>"
				+"</tr>\n"
				+INSERT_ROW_HERE+"\n";
		return s;
	}

	private String calculatedHitsRowGenerator(int rowNumber, String peptide, String protein, String alt, boolean graphs){
		String graphFileLocation = model.getSuperPath()+"Graphs"+File.separator+"Image "+rowNumber+".png";
		String graphsString = (graphs) ? "<td><a class = \"pic\" href = \""+graphFileLocation+"\">Image "+rowNumber+".png</a></td>\n" : "<td>N/A</td>";
		String s = "<tr "+alt+" >\n"
				+"<td>"+rowNumber+"</td>\n"
				+"<td>"+ peptide +"</td>\n"
				+"<td>"+ protein +"</td>\n"
				+ graphsString
				+"</tr>\n"
				+INSERT_ROW_HERE+"\n";
		return s;
	}

	private String allRunsRowGenerator(int rowNumber, String peptide, String protein, double midpoint, 
			double adjrsq, String alt, boolean graphs){
		String graphFileLocation = model.getSuperPath()+"Graphs"+File.separator+"Image "+rowNumber+".png";
		String graphsString = (graphs) ? "<td><a class = \"pic\" href = \""+graphFileLocation+"\">Image "+rowNumber+".png</a></td>\n" : "<td>N/A</td>";
		String s = "<tr "+alt+" >\n"
				+"<td>"+rowNumber+"</td>\n"
				+"<td>"+peptide+"</td>\n"
				+"<td>"+protein+"</td>\n"
				+"<td>"+midpoint+"</td>\n"
				+"<td>"+adjrsq+"</td>\n"
				+graphsString
				+"</tr>\n"
				+INSERT_ROW_HERE;
		return s;
	}
	private String allRunsRowGenerator(int rowNumber, String peptide, String protein, double midpoint, 
			double adjrsq, String alt, double midpoint2, double adjrsq2, boolean graphs){
		String graphFileLocation = model.getSuperPath()+"Graphs"+File.separator+"Image "+rowNumber+".png";
		String graphsString = (graphs) ? "<td><a class = \"pic\" href = \""+graphFileLocation+"\">Image "+rowNumber+".png</a></td>\n" : "<td>N/A</td>";
		String s = "<tr "+alt+" >\n"
				+"<td>"+rowNumber+"</td>\n"
				+"<td>"+peptide+"</td>\n"
				+"<td>"+protein+"</td>\n"
				+"<td>"+midpoint+"</td>\n"
				+"<td>"+adjrsq+"</td>\n"
				+"<td>"+midpoint2+"</td>\n"
				+"<td>"+adjrsq2+"</td>\n"
				+graphsString
				+"</tr>\n"
				+INSERT_ROW_HERE;
		return s;
	}

	/**
	 * 
	 * @return current date in human readable format
	 */
	private String getCurrentDate(){
		return new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(new Date());
	}

	public static void main(String[] args){
		HTMLGenerator hg = new HTMLGenerator(null,null);
		try {
			System.out.println(hg.loadBoilerplate(INITIAL_BOILERPLATE));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			hg.call();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}
