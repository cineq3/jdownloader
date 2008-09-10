//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypt;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class DDLMusicOrg extends PluginForDecrypt {
    final static String host = "ddl-music.org";

    private static final Pattern patternLink_Main = Pattern.compile("http://[\\w\\.]*?ddl-music\\.org/index\\.php\\?site=view_download&cat=.+&id=\\d+", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLink_Crypt = Pattern.compile("http://[\\w\\.]*?ddl-music\\.org/ddlm_cr\\.php\\?\\d+\\?\\d+", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternSupported = Pattern.compile(patternLink_Main.pattern() + "|" + patternLink_Crypt.pattern(), Pattern.CASE_INSENSITIVE);

    public DDLMusicOrg(String cfgName) {
        super(cfgName);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        try {

            if (new Regex(parameter, patternLink_Crypt).matches()) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                }
                RequestInfo reqinfo = HTTP.getRequest(new URL(parameter.replace("ddlm_cr.php", "test2.php")), null, parameter, false);
                String link = new Regex(reqinfo.getHtmlCode(), "<form action=\"(.*?)\" method=\"post\">", Pattern.CASE_INSENSITIVE).getMatch(0);
                if (link == null) { return null; }
                decryptedLinks.add(createDownloadlink(link));
            } else if (new Regex(parameter, patternLink_Main).matches()) {
                RequestInfo reqinfo = HTTP.getRequest(new URL(parameter));
                // passwort auslesen
                String password = new Regex(reqinfo.getHtmlCode(), "<td class=\"normalbold\"><div align=\"center\">Passwort</div></td>\n.*?</tr>\n.*?<tr>\n.*?<td class=\"normal\"><div align=\"center\">(.*?)</div></td>", Pattern.CASE_INSENSITIVE).getMatch(0);
                if (password != null && password.contains("kein Passwort")) {
                    password = null;
                }

                String ids[] = new Regex(reqinfo.getHtmlCode(), "href=\"(.*?)\n?\" target=\"\\_blank\" onmouseout=\"MM_swapImgRestore", Pattern.CASE_INSENSITIVE).getColumn(0);

                progress.setRange(ids.length);
                DownloadLink link;
                for (int i = 0; i < ids.length; i++) {
                    if (ids[i].startsWith("/ddlm_cr.php")) {
                        link = createDownloadlink("http://ddl-music.org" + ids[i]);
                    } else {
                        link = createDownloadlink(ids[i]);
                        try {
                            Thread.sleep(250);
                        } catch (InterruptedException e) {
                        }
                    }
                    link.addSourcePluginPassword(password);
                    decryptedLinks.add(link);
                    progress.increase(1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}