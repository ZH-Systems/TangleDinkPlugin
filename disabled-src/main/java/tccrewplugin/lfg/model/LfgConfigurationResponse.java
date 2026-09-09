package tccrewplugin.lfg.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LfgConfigurationResponse
{
	@SerializedName("categories")
	private List<LfgCategory> categories = new ArrayList<>();
	@SerializedName("message")
	private String message;
}
