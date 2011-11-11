package com.axelby.podax;

import java.io.File;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;

public class PodcastCursor {

	private Context _context;
	private Cursor _cursor;
	
	private Integer _idColumn = null;
	private Integer _titleColumn = null;
	private Integer _subscriptionTitleColumn = null;
	private Integer _queuePositionColumn = null;
	private Integer _mediaUrlColumn = null;
	private Integer _fileSizeColumn = null;
	private Integer _descriptionColumn = null;
	private Integer _lastPositionColumn = null;
	private Integer _durationColumn = null;

	public PodcastCursor(Context context, Cursor cursor) {
		_context = context;
		_cursor = cursor;
		if (_cursor.isBeforeFirst())
			_cursor.moveToFirst();

		_idColumn = cursor.getColumnIndex(PodcastProvider.COLUMN_ID);
		_subscriptionTitleColumn = cursor.getColumnIndex(PodcastProvider.COLUMN_SUBSCRIPTION_TITLE);
		_queuePositionColumn = cursor.getColumnIndex(PodcastProvider.COLUMN_QUEUE_POSITION);
		_mediaUrlColumn = cursor.getColumnIndex(PodcastProvider.COLUMN_MEDIA_URL);

		if (getId() != null)
			cursor.setNotificationUri(_context.getContentResolver(), getContentUri());
	}

	public Uri getContentUri() {
		if (getId() == null)
			return null;
		return ContentUris.withAppendedId(PodcastProvider.URI, getId());
	}

	public void registerContentObserver(ContentObserver observer) {
		if (getId() == null)
			return;
		_context.getContentResolver().registerContentObserver(getContentUri(), false, observer);
	}

	public Long getId() {
		if (_idColumn == null)
			_idColumn = _cursor.getColumnIndex(PodcastProvider.COLUMN_ID);
		if (_idColumn == -1)
			return null;
		if (_cursor.isNull(_idColumn))
			return null;
		return _cursor.getLong(_idColumn);
	}

	public String getTitle() {
		if (_titleColumn == null)
			_titleColumn = _cursor.getColumnIndex(PodcastProvider.COLUMN_TITLE);
		if (_titleColumn == -1)
			return null;
		if (_cursor.isNull(_titleColumn))
			return null;
		return _cursor.getString(_titleColumn);
	}

	public String getSubscriptionTitle() {
		if (_subscriptionTitleColumn == null)
			_subscriptionTitleColumn = _cursor.getColumnIndex(PodcastProvider.COLUMN_SUBSCRIPTION_TITLE);
		if (_subscriptionTitleColumn == -1)
			return null;
		if (_cursor.isNull(_subscriptionTitleColumn))
			return null;
		return _cursor.getString(_subscriptionTitleColumn);
	}

	public String getMediaUrl() {
		if (_mediaUrlColumn == null)
			_mediaUrlColumn = _cursor.getColumnIndex(PodcastProvider.COLUMN_MEDIA_URL);
		if (_mediaUrlColumn == -1)
			return null;
		if (_cursor.isNull(_mediaUrlColumn))
			return null;
		return _cursor.getString(_mediaUrlColumn);
	}

	public Integer getFileSize() {
		if (_fileSizeColumn == null)
			_fileSizeColumn = _cursor.getColumnIndex(PodcastProvider.COLUMN_FILE_SIZE);
		if (_fileSizeColumn == -1)
			return null;
		if (_cursor.isNull(_fileSizeColumn))
			return null;
		return _cursor.getInt(_fileSizeColumn);
	}

	public Integer getQueuePosition() {
		if (_queuePositionColumn == null)
			_queuePositionColumn = _cursor.getColumnIndex(PodcastProvider.COLUMN_QUEUE_POSITION);
		if (_queuePositionColumn == -1)
			return null;
		if (_cursor.isNull(_queuePositionColumn))
			return null;
		return _cursor.getInt(_queuePositionColumn);
	}

	public String getDescription() {
		if (_descriptionColumn == null)
			_descriptionColumn = _cursor.getColumnIndex(PodcastProvider.COLUMN_DESCRIPTION);
		if (_descriptionColumn == -1)
			return null;
		if (_cursor.isNull(_descriptionColumn))
			return null;
		return _cursor.getString(_descriptionColumn);
	}

	public Integer getLastPosition() {
		if (_lastPositionColumn == null)
			_lastPositionColumn = _cursor.getColumnIndex(PodcastProvider.COLUMN_LAST_POSITION);
		if (_lastPositionColumn == -1)
			return null;
		if (_cursor.isNull(_lastPositionColumn))
			return null;
		return _cursor.getInt(_lastPositionColumn);
	}

	public Integer getDuration() {
		if (_durationColumn == null)
			_durationColumn = _cursor.getColumnIndex(PodcastProvider.COLUMN_DURATION);
		if (_durationColumn == -1)
			return null;
		if (_cursor.isNull(_durationColumn))
			return null;
		return _cursor.getInt(_durationColumn);
	}

	public String getFilename() {
		return Podcast.getStoragePath() + String.valueOf(getId()) + "." + Podcast.getExtension(getMediaUrl());
	}

	public boolean isDownloaded() {
		if (getFileSize() == null)
			return false;
		File file = new File(getFilename());
		return file.exists() && file.length() == getFileSize() && getFileSize() != 0;
	}

	public void removeFromQueue() {
		ContentValues values = new ContentValues();
		values.put(PodcastProvider.COLUMN_QUEUE_POSITION, (Integer)null);
		_context.getContentResolver().update(getContentUri(), values, null, null);
	}

	public void addToQueue() {
		ContentValues values = new ContentValues();
		values.put(PodcastProvider.COLUMN_QUEUE_POSITION, Integer.MAX_VALUE);
		_context.getContentResolver().update(getContentUri(), values, null, null);
	}
}