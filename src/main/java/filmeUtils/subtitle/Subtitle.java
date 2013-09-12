package filmeUtils.subtitle;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;

import filmeUtils.commons.FileSystemUtils;
import filmeUtils.commons.OutputListener;
import filmeUtils.subtitle.subtitleSites.LegendasTv;
import filmeUtils.subtitle.subtitleSites.SubtitleLinkSearchCallback;
import filmeUtils.subtitle.subtitleSites.SubtitlePackageAndLink;
import filmeUtils.utils.RegexForSubPackageAndSubFile;
import filmeUtils.utils.RegexUtils;
import filmeUtils.utils.extraction.ExtractorImpl;
import filmeUtils.utils.http.SimpleHttpClient;

public class Subtitle {
	
	private final LegendasTv legendasTv;
	private OutputListener output;
	private final SimpleHttpClient httpclient;

	public Subtitle(final OutputListener output,final SimpleHttpClient httpclient,final LegendasTv legendasTv) {
		this.output = output;
		this.httpclient = httpclient;
		this.legendasTv = legendasTv;
	}

	public void search(final String searchTerm) {
		search(searchTerm,".*");
	}
	
	public void search(final String searchTerm, final String subtitleRegex) {
		legendasTv.search(searchTerm, new SubtitleLinkSearchCallback(){@Override public void process(final SubtitlePackageAndLink nameAndlink) {
				output.out(nameAndlink.name);
				final File tempDirWithSubtitles = downloadAndExtractToTempDirReturnUnzippedDirOrNull(nameAndlink.link);
				final String[] subtitlesFilenames = tempDirWithSubtitles.list();
				for (final String subtitlesFilename : subtitlesFilenames) {
					if(RegexUtils.matchesCaseInsensitive(subtitlesFilename, subtitleRegex))
						output.out(" -"+subtitlesFilename);
				}
		}});
	}

	public void download(final String searchTerm, final File destDir) {
		download(searchTerm,".*",destDir);
	}
	
	public void downloadNewer(final File destDir,final List<RegexForSubPackageAndSubFile> regexes, final List<String> ignoredPackages) {
		final SubtitleLinkSearchCallback searchListener = new SubtitleLinkSearchCallback() {	
			@Override
			public void process(final SubtitlePackageAndLink nameAndlink) {
				final String packageName = nameAndlink.name;
				if(ignoredPackages.contains(packageName)) return;
				final RegexForSubPackageAndSubFile regexMatchingPackageOrNull = RegexUtils.getRegexMatchingPackageOrNull(packageName,regexes);
				if(regexMatchingPackageOrNull == null) return;
				downloadSubtitlesMatchingRegexToDir(destDir, regexMatchingPackageOrNull.fileRegex , nameAndlink);
				FileSystemUtils.getInstance().addAlreadyDownloaded(packageName);
			}
		};
		legendasTv.getNewer(searchListener);
	}
	
	public void download(final String searchTerm, final String subtitleRegex,final File destDir) {
		final SubtitleLinkSearchCallback searchListener = new SubtitleLinkSearchCallback() {	
			@Override
			public void process(final SubtitlePackageAndLink nameAndlink) {
				downloadSubtitlesMatchingRegexToDir(destDir, subtitleRegex,nameAndlink);
			}
		};
		legendasTv.search(searchTerm, searchListener);
	}

	private void downloadSubtitlesMatchingRegexToDir(final File destDir, final String subtitleRegex,final SubtitlePackageAndLink nameAndlink) {
		
		output.out("Fazendo download de pacote de legendas "+nameAndlink.name);
		final String link = nameAndlink.link;
		
		File unzippedTempDestination = downloadAndExtractToTempDirReturnUnzippedDirOrNull(link);
		if(unzippedTempDestination == null){
			final boolean logged = false;
			int tries = 0;
			while(!logged && tries < 4){
				tries++;
				unzippedTempDestination = downloadAndExtractToTempDirReturnUnzippedDirOrNull(link);
				if(unzippedTempDestination == null){					
					output.out("Erro no download!!! Não logou no legendas.tv, tentando novamente...");
					legendasTv.login();
				}
			}
			if(!logged){
				output.out("Não foi possível logar no legendas.tv");
				System.exit(1);
			}
		}
		
		
		final List<String> filesThatMatches = FileSystemUtils.copyFilesMatchingRegexAndDeleteSourceDir(unzippedTempDestination,destDir, subtitleRegex);
		for (final String file : filesThatMatches) {
			output.out("Legenda "+file+" copiada para "+destDir.getAbsolutePath());
		}
	}

	public void listNewSubtitles() {
		legendasTv.getNewer(new SubtitleLinkSearchCallback(){@Override public void process(final SubtitlePackageAndLink nameAndlink) {
				output.out(nameAndlink.name);
		}});
	}

	private File downloadAndExtractToTempDirReturnUnzippedDirOrNull(final String link) {
		final File unzippedTempDestination;
		try {
			final File zipTempDestination = File.createTempFile("Filmeutils", "Filmeutils");
			unzippedTempDestination = File.createTempFile("Filmeutils", "Filmeutils");
			unzippedTempDestination.delete();
			unzippedTempDestination.mkdir();
			final String fileName = httpclient.getToFile(link, zipTempDestination);
			output.out("Download de pacote de legendas de "+link+" para "+zipTempDestination+" terminado.\nVerificando tipo de arquivo...");
			final ExtractorImpl extractor = new ExtractorImpl();

			if(fileName.toLowerCase().endsWith("rar")){
				output.out("Arquivo rar.");
				extractor.unrar(zipTempDestination, unzippedTempDestination);
			}else if(fileName.toLowerCase().endsWith("zip")){
				output.out("Arquivo zip.");
				extractor.unzip(zipTempDestination, unzippedTempDestination);
			}else{
				output.out("Arquivo inválido, zip ou rar esperado, mas retornou "+fileName);
				final String fileContents = FileUtils.readFileToString(zipTempDestination);
				output.out("Conteúdo do retorno:\n"+fileContents);
				return null;				
			}
			output.out("Pacote de legendas descompactado.");
			zipTempDestination.delete();
		}catch(final IOException e){throw new RuntimeException(e);}
		
		return unzippedTempDestination;
	}

	public void setOutputListener(final OutputListener outputListener) {
		output = outputListener;
	}

}
