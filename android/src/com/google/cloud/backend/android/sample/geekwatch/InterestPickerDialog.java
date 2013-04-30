package com.google.cloud.backend.android.sample.geekwatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.google.cloud.backend.android.R;

public class InterestPickerDialog extends DialogFragment {

	public interface HasGeekInterest {
		String getSelectedInterest();
		void setSelectedInterest(String interest);
	}

	private String[] interests;

	public InterestPickerDialog() {
		// No-arg constructor required for DialogFragment
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		interests = getResources().getStringArray(R.array.interests);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final HasGeekInterest activity = (HasGeekInterest) getActivity();
		int selected = getInterestIdx(activity.getSelectedInterest());
		Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle("What kind of geek are you?");
		builder.setSingleChoiceItems(interests, selected,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						activity.setSelectedInterest(interests[which]);
						dismiss();
					}
				});
		return builder.create();
	}

	private int getInterestIdx(String interestName) {
		for (int i = 0; i < interests.length; i++) {
			if (interests[i].equals(interestName)) {
				return i;
			}
		}
		return -1; // none selected
	}

}
