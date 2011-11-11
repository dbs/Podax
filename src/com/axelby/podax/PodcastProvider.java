package com.axelby.podax;

import java.util.Arrays;
import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class PodcastProvider extends ContentProvider {
	public static String AUTHORITY = "com.axelby.podax.PodcastProvider";
	public static String BASE_PATH = "podcasts";
	public static Uri URI = Uri.parse("content://" + PodcastProvider.AUTHORITY
			+ "/" + PodcastProvider.BASE_PATH);
	public static final String ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/vnd.axelby.podcast";
	public static final String DIR_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/vnd.axelby.podcast";

	final static int PODCASTS = 1;
	final static int PODCASTS_QUEUE = 2;
	final static int PODCAST_ID = 3;

	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_TITLE = "title";
	public static final String COLUMN_SUBSCRIPTION_TITLE = "subscriptionTitle";
	public static final String COLUMN_QUEUE_POSITION = "queuePosition";
	public static final String COLUMN_MEDIA_URL = "mediaUrl";
	public static final String COLUMN_LINK = "link";
	public static final String COLUMN_PUB_DATE = "pubDate";
	public static final String COLUMN_DESCRIPTION = "description";
	public static final String COLUMN_FILE_SIZE = "fileSize";
	public static final String COLUMN_LAST_POSITION = "lastPosition";
	public static final String COLUMN_DURATION = "duration";

	static UriMatcher uriMatcher;

	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(PodcastProvider.AUTHORITY, "podcasts",
				PodcastProvider.PODCASTS);
		uriMatcher.addURI(PodcastProvider.AUTHORITY, "podcasts/queue",
				PodcastProvider.PODCASTS_QUEUE);
		uriMatcher.addURI(PodcastProvider.AUTHORITY, "podcasts/#",
				PodcastProvider.PODCAST_ID);
	}

	DBAdapter _dbAdapter;

	@Override
	public boolean onCreate() {
		_dbAdapter = new DBAdapter(getContext());
		return true;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case PodcastProvider.PODCASTS:
		case PodcastProvider.PODCASTS_QUEUE:
			return PodcastProvider.DIR_TYPE;
		case PodcastProvider.PODCAST_ID:
			return PodcastProvider.ITEM_TYPE;
		}
		throw new IllegalArgumentException("Unknown URI");
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		HashMap<String, String> columnMap = new HashMap<String, String>();
		columnMap.put(COLUMN_ID, "podcasts._id AS _id");
		columnMap.put(COLUMN_TITLE, "podcasts.title AS title");
		columnMap.put(COLUMN_SUBSCRIPTION_TITLE,
				"subscriptions.title AS subscriptionTitle");
		columnMap.put(COLUMN_QUEUE_POSITION, "queuePosition");
		columnMap.put(COLUMN_MEDIA_URL, "mediaUrl");
		columnMap.put(COLUMN_LINK, "link");
		columnMap.put(COLUMN_PUB_DATE, "pubDate");
		columnMap.put(COLUMN_DESCRIPTION, "description");
		columnMap.put(COLUMN_FILE_SIZE, "fileSize");
		columnMap.put(COLUMN_LAST_POSITION, "lastPosition");
		columnMap.put(COLUMN_DURATION, "duration");

		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		sqlBuilder.setProjectionMap(columnMap);
		if (Arrays.asList(projection).contains(COLUMN_SUBSCRIPTION_TITLE))
			sqlBuilder
					.setTables("podcasts JOIN subscriptions on podcasts.subscriptionId = subscriptions._id");
		else
			sqlBuilder.setTables("podcasts");

		switch (uriMatcher.match(uri)) {
		case PodcastProvider.PODCASTS:
			break;
		case PodcastProvider.PODCASTS_QUEUE:
			sqlBuilder.appendWhere("queuePosition IS NOT NULL");
			if (sortOrder == null)
				sortOrder = "queuePosition";
			break;
		case PodcastProvider.PODCAST_ID:
			sqlBuilder.appendWhere("podcasts._id = " + uri.getLastPathSegment());
			break;
		default:
			throw new IllegalArgumentException("Unknown URI");
		}

		Cursor c = sqlBuilder.query(_dbAdapter.getRawDB(), projection,
				selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where,
			String[] whereArgs) {
		int count = 0;

		switch (uriMatcher.match(uri)) {
		case PODCAST_ID:
			String extraWhere = COLUMN_ID + " = " + uri.getLastPathSegment();
			if (where != null)
				where = extraWhere + " AND " + where;
			else
				where = extraWhere;
			break;
		default:
			throw new IllegalArgumentException("Unknown URI");
		}

		String podcastId = uri.getLastPathSegment();

		// subscription title is not in the table
		values.remove(COLUMN_SUBSCRIPTION_TITLE);

		// update queuePosition separately
		boolean notifyQueue = false;
		if (values.containsKey(COLUMN_QUEUE_POSITION)) {
			notifyQueue = true;

			// get the new position
			Integer newPosition = values.getAsInteger(COLUMN_QUEUE_POSITION);
			values.remove(COLUMN_QUEUE_POSITION);

			// no way to get changed record count until
			// SQLiteStatement.executeUpdateDelete in API level 11
			updateQueuePosition(podcastId, newPosition);
		}

		if (values.size() > 0)
			count += _dbAdapter.getRawDB().update("podcasts", values, where,
					whereArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		if (notifyQueue)
			getContext().getContentResolver().notifyChange(
					Uri.withAppendedPath(URI, "queue"), null);
		return count;
	}

	public void updateQueuePosition(String podcastId, Integer newPosition) {
		SQLiteDatabase db = _dbAdapter.getRawDB();

		// get the old position
		Cursor c = db.query("podcasts", new String[] { "queuePosition" },
				"_id = ?", new String[] { podcastId }, null, null, null);
		c.moveToFirst();
		Integer oldPosition = Integer.MAX_VALUE;
		if (!c.isNull(0))
			oldPosition = c.getInt(0);
		c.close();

		// no need to remove from queue if it's not in queue
		if (oldPosition == null && newPosition == null)
			return;

		if (oldPosition == null && newPosition != null) {
			// new at 3: 1 2 3 4 5 do: 3++ 4++ 5++
			db.execSQL("UPDATE podcasts SET queuePosition = queuePosition + 1 "
					+ "WHERE queuePosition >= ?", new Object[] { newPosition });
		}
		if (oldPosition != null && newPosition == null) {
			// remove 3: 1 2 3 4 5 do: 4-- 5--
			db.execSQL("UPDATE podcasts SET queuePosition = queuePosition - 1 "
					+ "WHERE queuePosition > ?", new Object[] { oldPosition });
		} else if (oldPosition != newPosition) {
			// moving up: 1 2 3 4 5 2 -> 4: 3-- 4-- 2->4
			if (oldPosition < newPosition)
				db.execSQL(
						"UPDATE podcasts SET queuePosition = queuePosition - 1 "
								+ "WHERE queuePosition > ? AND queuePosition <= ?",
						new Object[] { oldPosition, newPosition });
			// moving down: 1 2 3 4 5 4 -> 2: 2++ 3++ 4->2
			if (newPosition < oldPosition)
				db.execSQL(
						"UPDATE podcasts SET queuePosition = queuePosition + 1 "
								+ "WHERE queuePosition >= ? AND queuePosition < ?",
						new Object[] { newPosition, oldPosition });
		}

		// if new position is max_value, put the podcast at the end
		if (newPosition != null && newPosition == Integer.MAX_VALUE) {
			Cursor max = db.rawQuery("SELECT COALESCE(MAX(queuePosition) + 1, 0) FROM podcasts", null);
			max.moveToFirst();
			newPosition = max.getInt(0);
			max.close();
		}

		// update specified podcast
		db.execSQL("UPDATE podcasts SET queuePosition = ? WHERE _id = ?",
				new Object[] { newPosition, podcastId });
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		// TODO Auto-generated method stub
		return 0;
	}
}