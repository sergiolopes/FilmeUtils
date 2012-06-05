package filmeUtils.swing;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;

import filmeUtils.Downloader;
import filmeUtils.FilmeUtilsConstants;
import filmeUtils.OutputListener;
import filmeUtils.SearchListener;
import filmeUtils.subtitleSites.LegendasTv;


public class SearchScreenNeeds {

	private static final String LOW   = "Resolu\u00E7\u00E3o normal";
	private static final String HIGH  = "Alta defini\u00E7\u00E3o";
	private static final String ALL   = "Todas as resolu\u00E7\u00F5es";
	private File subtitleFolder;
	private final LegendasTv legendasTv;
	private final Downloader downloader;
	private final MutableFilmeUtilsOptions filmeUtilsOptions;

	public SearchScreenNeeds(final LegendasTv legendasTv, final Downloader downloader) {
		this.legendasTv = legendasTv;
		this.downloader = downloader;
		filmeUtilsOptions = new MutableFilmeUtilsOptions();
		final File filmeUtilsFolder = FilmeUtilsConstants.filmeUtilsFolder();
		final File file = new File(filmeUtilsFolder,"subtitlefolder");
		if(file.exists()){
				try {
					final String readFileToString = FileUtils.readFileToString(file);
					setSubtitleFolder(new File(readFileToString));
				} catch (final IOException e) {
					setSubtitleFolder(new File(System.getProperty("user.home")));
				}
		}else{			
			setSubtitleFolder(new File(System.getProperty("user.home")));
		}
	}
	
	public void download(final String item, final DownloadCallback callback) {
		new Thread(){
			@Override
			public void run() {
				final AtomicBoolean torrentWasFound = new AtomicBoolean(false);
				legendasTv.search(item, new SearchListener() {
					public boolean foundReturnIfShouldStopLooking(final String name, final String link) {
						final boolean success = downloader.download(name, link, filmeUtilsOptions);
						if(success){
							torrentWasFound.set(true);
						}
						return success;
					}
				});
				callback.done(torrentWasFound.get());
			}
		}.start();
	}

	public void setSubtitleFolder(final File folder) {
		final File filmeUtilsFolder = FilmeUtilsConstants.filmeUtilsFolder();
		final File file = new File(filmeUtilsFolder,"subtitlefolder");
		try {
			file.createNewFile();
			FileUtils.writeStringToFile(file, folder.getAbsolutePath());
		} catch (final IOException e) {
			//don't care
		}
		subtitleFolder = folder;
		filmeUtilsOptions.setSubtitlesDestinationFolder(folder);
	}

	public String getSubtitleFolder() {
		return subtitleFolder.getAbsolutePath();
	}

	public void getNewAddsList(final SearchCallback callback) {
		
		new Thread(){
			@Override
			public void run() {				
				legendasTv.getNewer(50, new SearchListener(){
					public boolean foundReturnIfShouldStopLooking(final String name,final String link) {
						callback.found(name);
						return false;
					}
					
				});
				callback.done();
			};
		}.start();
	}

	public void getResultsFor(final String text, final SearchCallback callback) {
		
		new Thread(){
			@Override
			public void run() {				
				legendasTv.search(text, new SearchListener(){
					public boolean foundReturnIfShouldStopLooking(final String name,final String link) {
						callback.found(name);
						return false;
					}
					
				});
				callback.done();
			}
		}.start();
	}

	public void setResolution(final String resolution) {
		if(resolution.equals(ALL)){
			filmeUtilsOptions.setShouldRefuseHD(false);
			filmeUtilsOptions.setShouldRefuseNonHD(false);
		}
		if(resolution.equals(HIGH)){
			filmeUtilsOptions.setShouldRefuseHD(false);
			filmeUtilsOptions.setShouldRefuseNonHD(true);
		}
		if(resolution.equals(LOW)){
			filmeUtilsOptions.setShouldRefuseHD(true);
			filmeUtilsOptions.setShouldRefuseNonHD(false);
		}
	}

	public String allResolutionsString() {
		return ALL;
	}

	public String highResolutionString() {
		return HIGH;
	}

	public String lowResolutionString() {
		return LOW;
	}

	public void setOutputListener(final OutputListener outputListener) {
		downloader.setOutputListener(outputListener);
	}

}
